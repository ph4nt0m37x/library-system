package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class ReturnLoanCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    val returnedAt: ZonedDateTime
)
