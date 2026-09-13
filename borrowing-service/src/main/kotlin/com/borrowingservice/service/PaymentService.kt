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
        @Suppress("UNUSED_PARAMETER") quotedAt: ZonedDateTime? = null
    ): PaymentQuote

    fun recordPayment(command: RecordPaymentCommand): CompletableFuture<Boolean>
}
