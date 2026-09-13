package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.math.BigDecimal
import java.time.ZonedDateTime

data class SettleFeeCommand(
    @TargetAggregateIdentifier
    val feeId: String,
    val paymentId: String,
    val allocationId: String,
    val amount: BigDecimal,
    @Deprecated("Fee settlement uses the server Clock")
    val settledAt: ZonedDateTime? = null
)
