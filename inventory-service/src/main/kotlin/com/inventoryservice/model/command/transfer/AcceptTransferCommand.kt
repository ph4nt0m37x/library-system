package com.inventoryservice.model.command.transfer

import com.inventoryservice.model.valueObject.TransferId
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class AcceptTransferCommand(
    @TargetAggregateIdentifier
    val id: TransferId,
    val reviewedBy: String
)