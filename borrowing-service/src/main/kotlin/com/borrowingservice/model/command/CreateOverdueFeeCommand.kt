package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class CreateOverdueFeeCommand(
    @TargetAggregateIdentifier
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val dueAt: ZonedDateTime,
    val returnedAt: ZonedDateTime,
    val createdAt: ZonedDateTime
)
