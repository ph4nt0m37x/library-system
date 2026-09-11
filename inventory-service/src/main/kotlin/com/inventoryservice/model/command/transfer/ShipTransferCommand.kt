package com.inventoryservice.model.command.transfer


import com.inventoryservice.model.valueObject.TransferId
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class ShipTransferCommand(
    @TargetAggregateIdentifier
    val id: TransferId
)