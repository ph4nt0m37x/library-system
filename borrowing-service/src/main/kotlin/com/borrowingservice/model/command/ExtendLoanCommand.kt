package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class ExtendLoanCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    @Deprecated("Normal loan extension uses the server Clock")
    val extendedAt: ZonedDateTime? = null
)
