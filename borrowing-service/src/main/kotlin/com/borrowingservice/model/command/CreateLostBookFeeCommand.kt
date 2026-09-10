package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class CreateLostBookFeeCommand(
    @TargetAggregateIdentifier
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val declaredLostAt: ZonedDateTime,
    val createdAt: ZonedDateTime
)
