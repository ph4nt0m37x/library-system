package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class RecordPermanentBookDamageCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    @Deprecated("Normal damage recording uses the server Clock")
    val damageRecordedAt: ZonedDateTime? = null
)
