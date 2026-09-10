package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class CreateLoanCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val borrowedAt: ZonedDateTime
)
