package com.inventoryservice.model.event.transfer


import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId

data class TransferShippedEvent(
    override val id: TransferId,
    val sourceLibraryId: LibraryId,
    val bookId: String,
    val quantity: Int
) : TransferEvent(id) {

    constructor(
        command: ShipTransferCommand,
        sourceLibraryId: LibraryId,
        bookId: String,
        quantity: Int
    ) : this(
        id = command.id,
        sourceLibraryId = sourceLibraryId,
        bookId = bookId,
        quantity = quantity
    )
}