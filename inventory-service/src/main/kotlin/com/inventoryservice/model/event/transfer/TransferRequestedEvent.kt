package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId

data class TransferRequestedEvent(
    override val id: TransferId,
    val sourceLibraryId: LibraryId,
    val destinationLibraryId: LibraryId,
    val bookId: String,
    val quantity: Int,
    val requestedBy: String
) : TransferEvent(id) {

    constructor(command: RequestTransferCommand) : this(
        id = TransferId(),
        sourceLibraryId = command.sourceLibraryId,
        destinationLibraryId = command.destinationLibraryId,
        bookId = command.bookId,
        quantity = command.quantity,
        requestedBy = command.requestedBy
    )
}