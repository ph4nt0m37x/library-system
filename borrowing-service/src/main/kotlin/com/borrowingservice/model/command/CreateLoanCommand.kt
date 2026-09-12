package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class CreateLoanCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    val memberId: String,
    val bookId: String,
    @Deprecated("Normal loan creation uses the server Clock")
    val borrowedAt: ZonedDateTime? = null,
    val idempotencyKey: String? = null
)
