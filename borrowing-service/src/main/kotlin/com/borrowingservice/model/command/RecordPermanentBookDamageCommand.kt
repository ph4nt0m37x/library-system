package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class RecordPermanentBookDamageCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    val damageRecordedAt: ZonedDateTime
)
