package com.inventoryservice.model.command.transfer

import com.inventoryservice.model.valueObject.LibraryId
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class RequestTransferCommand(
    @TargetAggregateIdentifier
    val sourceLibraryId: LibraryId,
    val destinationLibraryId: LibraryId,
    val bookId: String,
    val quantity: Int,
    val requestedBy: String
)