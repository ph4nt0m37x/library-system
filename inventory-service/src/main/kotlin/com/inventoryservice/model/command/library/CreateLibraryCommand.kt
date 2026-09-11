package com.inventoryservice.model.command.library

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class CreateLibraryCommand(
    @TargetAggregateIdentifier
    val name: String,
    val address: String
)