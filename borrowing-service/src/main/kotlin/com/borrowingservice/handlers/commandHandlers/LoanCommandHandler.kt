package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.client.MembershipClient
import com.borrowingservice.client.MembershipMemberNotFoundException
import com.borrowingservice.client.MembershipServiceUnavailableException
import com.borrowingservice.client.InventoryAvailabilityClient
import com.borrowingservice.client.InventoryResourceNotFoundException
import com.borrowingservice.client.InventoryServiceUnavailableException
import com.borrowingservice.config.LoanPolicyConfiguration
import com.borrowingservice.model.aggregate.Loan
import com.borrowingservice.model.command.CreateLoanCommand
import com.borrowingservice.model.command.DeclareBookLostCommand
import com.borrowingservice.model.command.ExtendLoanCommand
import com.borrowingservice.model.command.RecordPermanentBookDamageCommand
import com.borrowingservice.model.command.ReturnLoanCommand
import com.borrowingservice.model.event.LoanCreatedEvent
import com.borrowingservice.model.event.LoanExtendedEvent
import com.borrowingservice.model.event.LoanMarkedDamagedEvent
import com.borrowingservice.model.event.LoanMarkedLostEvent
import com.borrowingservice.model.event.LoanReturnedEvent
import com.borrowingservice.model.valueObject.LoanEligibilityException
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.model.valueObject.StateConflictException
import com.borrowingservice.model.valueObject.enums.FeeStatus
import com.borrowingservice.model.valueObject.enums.LoanRejectionReason
import com.borrowingservice.model.valueObject.enums.LoanStatus
import com.borrowingservice.repository.BorrowingBanRecordRepository
import com.borrowingservice.repository.FeeRepository
import com.borrowingservice.repository.LoanRepository
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime

@Component
class LoanCommandHandler(
    @Qualifier("axonLoanRepository") private val loanAggregateRepository: Repository<Loan>,
    private val loanRepository: LoanRepository,
    private val feeRepository: FeeRepository,
    private val banRecordRepository: BorrowingBanRecordRepository,
    private val membershipClient: MembershipClient,
    private val inventoryAvailabilityClient: InventoryAvailabilityClient,
    private val policy: LoanPolicyConfiguration,
    private val clock: Clock
) {
    @CommandHandler
    @Transactional
    fun handle(command: CreateLoanCommand): String {
        val idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey)
        val memberId = normalizeIdentifier(command.memberId, "memberId")
        val bookId = normalizeIdentifier(command.bookId, "bookId")
        val libraryId = normalizeIdentifier(command.libraryId, "libraryId")

        loanRepository.findByIdempotencyKey(idempotencyKey)?.let { existing ->
            if (
                existing.memberId == memberId && existing.bookId == bookId &&
                existing.libraryId == libraryId
            ) {
                return existing.loanId
            }
            throw StateConflictException(
                "IDEMPOTENCY_KEY_REUSED",
                "Idempotency key $idempotencyKey was already used for a different loan payload"
            )
        }

        if (loanRepository.existsById(command.loanId)) {
            throw StateConflictException("LOAN_ID_ALREADY_EXISTS", "Loan ${command.loanId} already exists")
        }
        val borrowedAt = now()
        checkEligibility(memberId, borrowedAt)
        checkStockAvailability(libraryId, bookId)

        val event = LoanCreatedEvent(
            loanId = command.loanId,
            memberId = memberId,
            bookId = bookId,
            libraryId = libraryId,
            borrowedAt = borrowedAt,
            dueAt = borrowedAt.plus(policy.regularLoanDuration),
            idempotencyKey = idempotencyKey
        )
        loanAggregateRepository.newInstance {
            Loan().also { AggregateLifecycle.apply(event) }
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: ExtendLoanCommand): String {
        requireLoan(command.loanId)
        loanAggregateRepository.load(command.loanId).execute { loan ->
            if (loan.status != LoanStatus.ACTIVE) {
                throw StateConflictException("LOAN_NOT_ACTIVE", "Only an ACTIVE Loan can be extended")
            }
            if (loan.extendedAt != null) {
                throw StateConflictException("LOAN_ALREADY_EXTENDED", "A Loan can be extended only once")
            }
            val extendedAt = now()
            require(!extendedAt.isBefore(loan.borrowedAt)) {
                "A Loan cannot be extended before it was borrowed"
            }
            if (!extendedAt.isBefore(loan.dueAt)) {
                throw StateConflictException(
                    "LOAN_EXTENSION_NOT_ALLOWED",
                    "A Loan cannot be extended at or after its dueAt"
                )
            }
            AggregateLifecycle.apply(
                LoanExtendedEvent(
                    loanId = loan.loanId,
                    extendedAt = extendedAt,
                    dueAt = loan.dueAt.plus(policy.extensionDuration)
                )
            )
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: ReturnLoanCommand): String {
        requireLoan(command.loanId)
        loanAggregateRepository.load(command.loanId).execute { loan ->
            if (loan.status != LoanStatus.ACTIVE) {
                throw StateConflictException("LOAN_NOT_ACTIVE", "Only an ACTIVE Loan can be returned")
            }
            val returnedAt = now()
            require(!returnedAt.isBefore(loan.borrowedAt)) {
                "A Loan cannot be returned before it was borrowed"
            }
            AggregateLifecycle.apply(
                LoanReturnedEvent(
                    loanId = loan.loanId,
                    memberId = loan.memberId,
                    bookId = loan.bookId,
                    libraryId = requireLibraryId(loan),
                    dueAt = loan.dueAt,
                    returnedAt = returnedAt,
                    idempotencyKey = loan.idempotencyKey
                )
            )
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: DeclareBookLostCommand): String {
        requireLoan(command.loanId)
        loanAggregateRepository.load(command.loanId).execute { loan ->
            if (loan.status != LoanStatus.ACTIVE) {
                throw StateConflictException("LOAN_NOT_ACTIVE", "Only an ACTIVE Loan can be declared LOST")
            }
            val declaredLostAt = now()
            require(!declaredLostAt.isBefore(loan.borrowedAt)) {
                "A Loan cannot be declared lost before it was borrowed"
            }
            AggregateLifecycle.apply(
                LoanMarkedLostEvent(
                    loanId = loan.loanId,
                    memberId = loan.memberId,
                    bookId = loan.bookId,
                    libraryId = requireLibraryId(loan),
                    declaredLostAt = declaredLostAt,
                    idempotencyKey = loan.idempotencyKey
                )
            )
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: RecordPermanentBookDamageCommand): String {
        requireLoan(command.loanId)
        loanAggregateRepository.load(command.loanId).execute { loan ->
            if (loan.status != LoanStatus.ACTIVE) {
                throw StateConflictException("LOAN_NOT_ACTIVE", "Only an ACTIVE Loan can be marked DAMAGED")
            }
            val damageRecordedAt = now()
            require(!damageRecordedAt.isBefore(loan.borrowedAt)) {
                "A Loan cannot be marked damaged before it was borrowed"
            }
            AggregateLifecycle.apply(
                LoanMarkedDamagedEvent(
                    loanId = loan.loanId,
                    memberId = loan.memberId,
                    bookId = loan.bookId,
                    libraryId = requireLibraryId(loan),
                    damageRecordedAt = damageRecordedAt,
                    idempotencyKey = loan.idempotencyKey
                )
            )
        }
        return command.loanId
    }

    private fun checkEligibility(memberId: String, at: ZonedDateTime) {
        val membership = try {
            membershipClient.subscriptionEligibility(memberId)
        } catch (exception: MembershipMemberNotFoundException) {
            throw exception
        } catch (exception: MembershipServiceUnavailableException) {
            throw exception
        } catch (exception: Exception) {
            causeOf<MembershipMemberNotFoundException>(exception)?.let { throw it }
            throw MembershipServiceUnavailableException(exception)
        }

        if (!membership.exists) {
            throw MembershipMemberNotFoundException()
        }
        if (membership.memberId != memberId) {
            throw MembershipServiceUnavailableException()
        }
        rejectUnless(
            membership.active,
            LoanRejectionReason.MEMBERSHIP_INACTIVE
        )
        rejectUnless(
            loanRepository.countByMemberIdAndStatus(memberId, LoanStatus.ACTIVE) < policy.maxActiveLoans,
            LoanRejectionReason.ACTIVE_LOAN_LIMIT_REACHED
        )
        rejectUnless(
            feeRepository.countByMemberIdAndStatus(memberId, FeeStatus.UNPAID) < policy.maxUnpaidFees,
            LoanRejectionReason.TOO_MANY_UNPAID_FEES
        )

        banRecordRepository.findByMemberId(memberId)?.let { record ->
            rejectUnless(!record.hasPermanentBan(), LoanRejectionReason.MEMBER_PERMANENTLY_BANNED)
            rejectUnless(!record.hasActiveTemporaryBan(at), LoanRejectionReason.MEMBER_TEMPORARILY_BANNED)
        }
    }

    private fun rejectUnless(eligible: Boolean, reason: LoanRejectionReason) {
        if (!eligible) {
            throw LoanEligibilityException(reason)
        }
    }

    private fun checkStockAvailability(libraryId: String, bookId: String) {
        val stock = try {
            inventoryAvailabilityClient.availability(libraryId, bookId)
        } catch (exception: InventoryResourceNotFoundException) {
            throw exception
        } catch (exception: InventoryServiceUnavailableException) {
            throw exception
        } catch (exception: Exception) {
            causeOf<InventoryResourceNotFoundException>(exception)?.let { throw it }
            throw InventoryServiceUnavailableException(exception)
        }

        if (stock.libraryId != libraryId || stock.bookId != bookId) {
            throw InventoryServiceUnavailableException()
        }
        if (!stock.libraryActive) {
            throw StateConflictException("LIBRARY_INACTIVE", "Library $libraryId is not active")
        }
        if (!stock.bookActive) {
            throw ResourceNotFoundException("Book", bookId)
        }
        if (!stock.available || stock.availableQuantity <= 0) {
            throw StateConflictException(
                "COPY_UNAVAILABLE",
                "No copy of book $bookId is available at library $libraryId"
            )
        }
    }

    private fun requireLoan(loanId: String) {
        if (!loanRepository.existsById(loanId)) {
            throw ResourceNotFoundException("Loan", loanId)
        }
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)

    private fun normalizeIdempotencyKey(value: String?): String {
        val normalized = value?.trim()
        require(!normalized.isNullOrEmpty()) { "idempotencyKey must be provided" }
        require(normalized.length <= 100) { "idempotencyKey must not exceed 100 characters" }
        return normalized
    }

    private fun normalizeIdentifier(value: String, field: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "$field must not be blank" }
        require(normalized.length <= 100) { "$field must not exceed 100 characters" }
        return normalized
    }

    private fun requireLibraryId(loan: Loan): String = loan.libraryId
        ?: throw StateConflictException(
            "LOAN_LIBRARY_UNKNOWN",
            "Loan ${loan.loanId} predates library-aware circulation and cannot update stock"
        )

    private inline fun <reified T : Throwable> causeOf(exception: Throwable): T? {
        var current: Throwable? = exception
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }
}
