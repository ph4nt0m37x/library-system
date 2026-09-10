package com.borrowingservice.handlers.eventHandlers

import com.borrowingservice.model.event.PaymentRecordedEvent
import com.borrowingservice.model.view.PaymentAllocationView
import com.borrowingservice.model.view.PaymentView
import com.borrowingservice.repository.PaymentViewRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ProcessingGroup("borrowing-projections")
class PaymentEventHandler(
    private val paymentViewRepository: PaymentViewRepository
) {
    @EventHandler
    @Transactional
    fun on(event: PaymentRecordedEvent) {
        if (paymentViewRepository.existsById(event.paymentId)) {
            return
        }

        paymentViewRepository.save(
            PaymentView(
                paymentId = event.paymentId,
                memberId = event.memberId,
                amount = event.amount,
                currency = event.currency,
                paidAt = event.paidAt,
                allocations = event.allocations.mapTo(mutableListOf()) { allocation ->
                    PaymentAllocationView(
                        allocationId = allocation.allocationId,
                        paymentId = event.paymentId,
                        feeId = allocation.feeId,
                        amount = allocation.amount
                    )
                }
            )
        )
    }
}
