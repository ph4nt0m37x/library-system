package com.borrowingservice.service.impl

import com.borrowingservice.model.command.RecordPaymentCommand
import com.borrowingservice.service.FeeQuoteService
import com.borrowingservice.service.PaymentQuote
import com.borrowingservice.service.PaymentService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

@Service
class PaymentServiceImpl(
    private val commandGateway: CommandGateway,
    private val feeQuoteService: FeeQuoteService
) : PaymentService {
    override fun quote(
        paymentId: String,
        memberId: String,
        feeIds: List<String>,
        currency: String,
        quotedAt: ZonedDateTime
    ): PaymentQuote = feeQuoteService.quote(paymentId, memberId, feeIds, currency, quotedAt)

    override fun recordPayment(command: RecordPaymentCommand): CompletableFuture<String> = commandGateway.send(command)
}
