package com.inventoryservice.model.command.library

import org.axonframework.modelling.command.TargetAggregateIdentifier
import com.inventoryservice.model.valueObject.LibraryId

data class UpdateLibraryCommand(
    @TargetAggregateIdentifier
    val id: LibraryId,
    val name: String,
    val address: String
)