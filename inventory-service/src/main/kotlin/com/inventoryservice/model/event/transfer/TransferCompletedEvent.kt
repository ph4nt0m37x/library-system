package com.inventoryservice.model.event.transfer


import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId

data class TransferCompletedEvent(
    override val id: TransferId,
    val destinationLibraryId: LibraryId,
    val bookId: String,
    val quantity: Int
) : TransferEvent(id) {

    constructor(
        command: CompleteTransferCommand,
        destinationLibraryId: LibraryId,
        bookId: String,
        quantity: Int
    ) : this(
        id = command.id,
        destinationLibraryId = destinationLibraryId,
        bookId = bookId,
        quantity = quantity
    )
}