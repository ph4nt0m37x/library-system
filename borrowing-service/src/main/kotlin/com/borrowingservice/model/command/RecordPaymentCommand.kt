package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.math.BigDecimal
import java.time.ZonedDateTime

data class RecordPaymentCommand(
    @TargetAggregateIdentifier
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    @Deprecated("Normal payment recording uses the server Clock")
    val paidAt: ZonedDateTime? = null,
    val feeIds: List<String>
)
