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
    val paidAt: ZonedDateTime,
    val feeIds: List<String>
)
