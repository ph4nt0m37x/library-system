package com.borrowingservice.handlers.eventHandlers

import com.borrowingservice.model.event.DamagedBookFeeCreatedEvent
import com.borrowingservice.model.event.FeeSettledEvent
import com.borrowingservice.model.event.LostBookFeeCreatedEvent
import com.borrowingservice.model.event.OverdueFeeCreatedEvent
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import com.borrowingservice.model.view.FeeView
import com.borrowingservice.repository.FeeViewRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ProcessingGroup("borrowing-projections")
class FeeEventHandler(
    private val feeViewRepository: FeeViewRepository
) {
    @EventHandler
    @Transactional
    fun on(event: OverdueFeeCreatedEvent) {
        createIfMissing(
            FeeView(
                feeId = event.feeId,
                loanId = event.loanId,
                memberId = event.memberId,
                currency = event.currency,
                reason = FeeReason.OVERDUE,
                status = FeeStatus.UNPAID,
                createdAt = event.createdAt,
                dueAt = event.dueAt,
                returnedAt = event.returnedAt
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LostBookFeeCreatedEvent) {
        createIfMissing(
            FeeView(
                feeId = event.feeId,
                loanId = event.loanId,
                memberId = event.memberId,
                currency = event.currency,
                reason = FeeReason.LOST,
                status = FeeStatus.UNPAID,
                createdAt = event.createdAt,
                incidentAt = event.declaredLostAt
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: DamagedBookFeeCreatedEvent) {
        createIfMissing(
            FeeView(
                feeId = event.feeId,
                loanId = event.loanId,
                memberId = event.memberId,
                currency = event.currency,
                reason = FeeReason.DAMAGED,
                status = FeeStatus.UNPAID,
                createdAt = event.createdAt,
                incidentAt = event.damageRecordedAt
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: FeeSettledEvent) {
        val view = feeViewRepository.findById(event.feeId)
            .orElseThrow { IllegalStateException("Fee projection not found: ${event.feeId}") }
        view.status = FeeStatus.PAID
        view.settledAt = event.settledAt
        view.settledByPaymentId = event.paymentId
        view.settlementAllocationId = event.allocationId
        view.settlementAmount = event.amount
        feeViewRepository.save(view)
    }

    private fun createIfMissing(view: FeeView) {
        if (!feeViewRepository.existsById(view.feeId)) {
            feeViewRepository.save(view)
        }
    }
}
