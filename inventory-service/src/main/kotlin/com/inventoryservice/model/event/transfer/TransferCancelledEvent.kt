package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId

data class TransferCancelledEvent(
    override val id: TransferId,
    val sourceLibraryId: LibraryId,
    val bookId: BookId,
    val quantity: Int,
    val releaseStock: Boolean,
    val reason: String? = null
) : TransferEvent(id) {

    constructor(
        command: CancelTransferCommand,
        sourceLibraryId: LibraryId,
        bookId: BookId,
        quantity: Int,
        releaseStock: Boolean
    ) : this(
        id = command.id,
        sourceLibraryId = sourceLibraryId,
        bookId = bookId,
        quantity = quantity,
        releaseStock = releaseStock,
        reason = command.reason
    )
}
