package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.model.aggregate.Fee
import com.borrowingservice.model.command.CreateDamagedBookFeeCommand
import com.borrowingservice.model.command.CreateLostBookFeeCommand
import com.borrowingservice.model.command.CreateOverdueFeeCommand
import com.borrowingservice.model.command.SettleFeeCommand
import com.borrowingservice.model.event.DamagedBookFeeCreatedEvent
import com.borrowingservice.model.event.FeeSettledEvent
import com.borrowingservice.model.event.LostBookFeeCreatedEvent
import com.borrowingservice.model.event.OverdueFeeCreatedEvent
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.model.valueObject.StateConflictException
import com.borrowingservice.repository.FeeRepository
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime

@Component
class FeeCommandHandler(
    @Qualifier("axonFeeRepository") private val feeAggregateRepository: Repository<Fee>,
    private val feeRepository: FeeRepository,
    private val clock: Clock
) {
    @CommandHandler
    @Transactional
    fun handle(command: CreateOverdueFeeCommand): String {
        existingFee(command.loanId, FeeReason.OVERDUE)?.let { return it }
        require(!command.createdAt.isAfter(now())) { "An OVERDUE Fee cannot be created in the future" }
        require(command.returnedAt.isAfter(command.dueAt)) { "An OVERDUE Fee requires a late return" }
        require(!command.createdAt.isBefore(command.returnedAt)) {
            "An OVERDUE Fee cannot be created before the loan was returned"
        }
        create(
            OverdueFeeCreatedEvent(
                command.feeId,
                command.loanId,
                command.memberId,
                normalizeCurrency(command.currency),
                command.dueAt,
                command.returnedAt,
                command.createdAt
            )
        )
        return command.feeId
    }

    @CommandHandler
    @Transactional
    fun handle(command: CreateLostBookFeeCommand): String {
        existingFee(command.loanId, FeeReason.LOST)?.let { return it }
        require(!command.createdAt.isAfter(now())) { "A LOST Fee cannot be created in the future" }
        require(!command.createdAt.isBefore(command.declaredLostAt)) {
            "A LOST Fee cannot be created before the book was declared lost"
        }
        create(
            LostBookFeeCreatedEvent(
                command.feeId,
                command.loanId,
                command.memberId,
                normalizeCurrency(command.currency),
                command.declaredLostAt,
                command.createdAt
            )
        )
        return command.feeId
    }

    @CommandHandler
    @Transactional
    fun handle(command: CreateDamagedBookFeeCommand): String {
        existingFee(command.loanId, FeeReason.DAMAGED)?.let { return it }
        require(!command.createdAt.isAfter(now())) { "A DAMAGED Fee cannot be created in the future" }
        require(!command.createdAt.isBefore(command.damageRecordedAt)) {
            "A DAMAGED Fee cannot be created before the damage was recorded"
        }
        create(
            DamagedBookFeeCreatedEvent(
                command.feeId,
                command.loanId,
                command.memberId,
                normalizeCurrency(command.currency),
                command.damageRecordedAt,
                command.createdAt
            )
        )
        return command.feeId
    }

    @CommandHandler
    @Transactional
    fun handle(command: SettleFeeCommand): String {
        require(command.amount.signum() > 0) { "A Fee settlement amount must be positive" }
        val current = feeRepository.findById(command.feeId)
            .orElseThrow { ResourceNotFoundException("Fee", command.feeId) }
        if (current.status == FeeStatus.PAID) {
            if (current.settledByPaymentId != command.paymentId ||
                current.settlementAllocationId != command.allocationId
            ) {
                throw StateConflictException(
                    "FEE_ALREADY_SETTLED",
                    "Fee ${command.feeId} has already been settled by another allocation"
                )
            }
            return command.feeId
        }
        val settledAt = now()
        require(!settledAt.isBefore(current.createdAt)) {
            "A Fee cannot be settled before it was created"
        }

        feeAggregateRepository.load(command.feeId).execute { fee ->
            if (fee.status == FeeStatus.PAID) {
                if (fee.settledByPaymentId != command.paymentId ||
                    fee.settlementAllocationId != command.allocationId
                ) {
                    throw StateConflictException(
                        "FEE_ALREADY_SETTLED",
                        "Fee ${command.feeId} has already been settled by another allocation"
                    )
                }
            } else {
                AggregateLifecycle.apply(
                    FeeSettledEvent(
                        feeId = command.feeId,
                        paymentId = command.paymentId,
                        allocationId = command.allocationId,
                        amount = command.amount,
                        settledAt = settledAt
                    )
                )
            }
        }
        return command.feeId
    }

    private fun existingFee(loanId: String, expectedReason: FeeReason): String? =
        feeRepository.findByLoanId(loanId)?.also { existing ->
            if (existing.reason != expectedReason) {
                throw StateConflictException(
                    "FEE_ALREADY_EXISTS_FOR_LOAN",
                    "Loan $loanId already has a ${existing.reason} Fee"
                )
            }
        }?.feeId

    private fun create(event: Any) {
        feeAggregateRepository.newInstance {
            Fee().also { AggregateLifecycle.apply(event) }
        }
    }

    private fun normalizeCurrency(currency: String): String {
        val normalized = currency.trim().uppercase()
        require(CURRENCY_PATTERN.matches(normalized)) { "Currency must be a three-letter ISO 4217 code" }
        return normalized
    }

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)

    private companion object {
        val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}
