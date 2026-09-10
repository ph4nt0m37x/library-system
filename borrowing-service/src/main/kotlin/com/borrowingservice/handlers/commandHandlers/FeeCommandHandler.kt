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
import com.borrowingservice.repository.FeeRepository
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FeeCommandHandler(
    @Qualifier("axonFeeRepository") private val feeAggregateRepository: Repository<Fee>,
    private val feeRepository: FeeRepository
) {
    @CommandHandler
    @Transactional
    fun handle(command: CreateOverdueFeeCommand): String {
        existingFee(command.loanId, FeeReason.OVERDUE)?.let { return it }
        require(command.returnedAt.isAfter(command.dueAt)) { "An OVERDUE Fee requires a late return" }
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
            .orElseThrow { IllegalArgumentException("Fee ${command.feeId} does not exist") }
        if (current.status == FeeStatus.PAID) {
            require(
                current.settledByPaymentId == command.paymentId &&
                    current.settlementAllocationId == command.allocationId
            ) { "Fee ${command.feeId} has already been settled by another allocation" }
            return command.feeId
        }

        feeAggregateRepository.load(command.feeId).execute { fee ->
            if (fee.status == FeeStatus.PAID) {
                require(
                    fee.settledByPaymentId == command.paymentId &&
                        fee.settlementAllocationId == command.allocationId
                ) { "Fee ${command.feeId} has already been settled by another allocation" }
            } else {
                AggregateLifecycle.apply(
                    FeeSettledEvent(
                        feeId = command.feeId,
                        paymentId = command.paymentId,
                        allocationId = command.allocationId,
                        amount = command.amount,
                        settledAt = command.settledAt
                    )
                )
            }
        }
        return command.feeId
    }

    private fun existingFee(loanId: String, expectedReason: FeeReason): String? =
        feeRepository.findByLoanId(loanId)?.also { existing ->
            require(existing.reason == expectedReason) {
                "Loan $loanId already has a ${existing.reason} Fee"
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

    private companion object {
        val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}
