package com.borrowingservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.ZonedDateTime

data class DeclareBookLostCommand(
    @TargetAggregateIdentifier
    val loanId: String,
    @Deprecated("Normal lost-book declaration uses the server Clock")
    val declaredLostAt: ZonedDateTime? = null
)
