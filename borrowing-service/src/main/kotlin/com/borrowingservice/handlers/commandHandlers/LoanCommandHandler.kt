package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.client.MembershipClient
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
    private val policy: LoanPolicyConfiguration,
    private val clock: Clock
) {
    @CommandHandler
    @Transactional
    fun handle(command: CreateLoanCommand): String {
        require(!loanRepository.existsById(command.loanId)) { "Loan ${command.loanId} already exists" }
        checkEligibility(command.memberId, ZonedDateTime.now(clock))

        val event = LoanCreatedEvent(
            loanId = command.loanId,
            memberId = command.memberId,
            bookId = command.bookId,
            borrowedAt = command.borrowedAt,
            dueAt = command.borrowedAt.plus(policy.regularLoanDuration)
        )
        loanAggregateRepository.newInstance {
            Loan().also { AggregateLifecycle.apply(event) }
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: ExtendLoanCommand): String {
        loanAggregateRepository.load(command.loanId).execute { loan ->
            require(loan.status == LoanStatus.ACTIVE) { "Only an ACTIVE Loan can be extended" }
            require(loan.extendedAt == null) { "A Loan can be extended only once" }
            require(command.extendedAt.isBefore(loan.dueAt)) { "A Loan cannot be extended at or after its dueAt" }
            AggregateLifecycle.apply(
                LoanExtendedEvent(
                    loanId = loan.loanId,
                    extendedAt = command.extendedAt,
                    dueAt = loan.dueAt.plus(policy.extensionDuration)
                )
            )
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: ReturnLoanCommand): String {
        loanAggregateRepository.load(command.loanId).execute { loan ->
            require(loan.status == LoanStatus.ACTIVE) { "Only an ACTIVE Loan can be returned" }
            AggregateLifecycle.apply(
                LoanReturnedEvent(
                    loanId = loan.loanId,
                    memberId = loan.memberId,
                    dueAt = loan.dueAt,
                    returnedAt = command.returnedAt
                )
            )
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: DeclareBookLostCommand): String {
        loanAggregateRepository.load(command.loanId).execute { loan ->
            require(loan.status == LoanStatus.ACTIVE) { "Only an ACTIVE Loan can be declared LOST" }
            AggregateLifecycle.apply(
                LoanMarkedLostEvent(loan.loanId, loan.memberId, command.declaredLostAt)
            )
        }
        return command.loanId
    }

    @CommandHandler
    @Transactional
    fun handle(command: RecordPermanentBookDamageCommand): String {
        loanAggregateRepository.load(command.loanId).execute { loan ->
            require(loan.status == LoanStatus.ACTIVE) { "Only an ACTIVE Loan can be marked DAMAGED" }
            AggregateLifecycle.apply(
                LoanMarkedDamagedEvent(loan.loanId, loan.memberId, command.damageRecordedAt)
            )
        }
        return command.loanId
    }

    private fun checkEligibility(memberId: String, at: ZonedDateTime) {
        rejectUnless(
            membershipClient.hasActiveSubscription(memberId),
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
}
