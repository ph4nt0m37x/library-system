package com.borrowingservice.service

import com.borrowingservice.model.command.RecordPaymentCommand
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

interface PaymentService {
    fun quote(
        paymentId: String,
        memberId: String,
        feeIds: List<String>,
        currency: String,
        quotedAt: ZonedDateTime
    ): PaymentQuote

    fun recordPayment(command: RecordPaymentCommand): CompletableFuture<String>
}
