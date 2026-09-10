package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class ExtendLoanCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    val extendedAt: ZonedDateTime
)
