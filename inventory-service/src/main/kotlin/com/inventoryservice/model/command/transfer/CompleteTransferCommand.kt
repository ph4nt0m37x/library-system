package com.inventoryservice.model.command.transfer

import com.inventoryservice.model.valueObject.TransferId
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class CompleteTransferCommand(
    @TargetAggregateIdentifier
    val id: TransferId,
    val completedAt: Instant? = null
)
