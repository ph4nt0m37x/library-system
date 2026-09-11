package com.inventoryservice.model.command.library

import org.axonframework.modelling.command.TargetAggregateIdentifier
import com.inventoryservice.model.valueObject.LibraryId

data class DeleteLibraryCommand(
    @TargetAggregateIdentifier
    val id: LibraryId
)