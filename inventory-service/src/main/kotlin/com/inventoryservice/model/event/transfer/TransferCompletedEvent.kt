package com.inventoryservice.model.event.transfer


import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import java.time.Instant

data class TransferCompletedEvent(
    override val id: TransferId,
    val destinationLibraryId: LibraryId,
    val bookId: BookId,
    val quantity: Int,
    val completedAt: Instant
) : TransferEvent(id) {

    constructor(
        command: CompleteTransferCommand,
        destinationLibraryId: LibraryId,
        bookId: BookId,
        quantity: Int
    ) : this(
        id = command.id,
        destinationLibraryId = destinationLibraryId,
        bookId = bookId,
        quantity = quantity,
        completedAt = requireNotNull(command.completedAt) {
            "completedAt must be set before publishing TransferCompletedEvent"
        }
    )
}
