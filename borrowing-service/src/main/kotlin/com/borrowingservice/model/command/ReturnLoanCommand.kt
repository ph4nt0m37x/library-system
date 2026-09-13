package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class ReturnLoanCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    @Deprecated("Normal loan return uses the server Clock")
    val returnedAt: ZonedDateTime? = null
)
