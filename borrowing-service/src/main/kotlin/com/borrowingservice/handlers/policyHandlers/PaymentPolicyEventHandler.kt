package com.borrowingservice.handlers.policyHandlers

import com.borrowingservice.model.command.SettleFeeCommand
import com.borrowingservice.model.event.PaymentRecordedEvent
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ProcessingGroup("borrowing-policies")
class PaymentPolicyEventHandler(
    private val commandGateway: CommandGateway
) {
    @EventHandler
    @Transactional
    fun on(event: PaymentRecordedEvent) {
        event.allocations.forEach { allocation ->
            commandGateway.sendAndWait<String>(
                SettleFeeCommand(
                    feeId = allocation.feeId,
                    paymentId = event.paymentId,
                    allocationId = allocation.allocationId,
                    amount = allocation.amount,
                    settledAt = event.paidAt
                )
            )
        }
    }
}
