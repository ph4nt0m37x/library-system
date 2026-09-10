package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class DeclareBookLostCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    val declaredLostAt: ZonedDateTime
)
