package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class CreateDamagedBookFeeCommand(
    @TargetAggregateIdentifier
    val feeId: String,
    val loanId: String,
    val memberId: String,
    val currency: String,
    val damageRecordedAt: ZonedDateTime,
    val createdAt: ZonedDateTime
)
