package com.inventoryservice.model.command.transfer

import com.inventoryservice.model.valueObject.TransferId
import jakarta.validation.constraints.Size
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class CancelTransferCommand(
    @TargetAggregateIdentifier
    val id: TransferId,
    @field:Size(max = 500)
    val reason: String? = null
)
