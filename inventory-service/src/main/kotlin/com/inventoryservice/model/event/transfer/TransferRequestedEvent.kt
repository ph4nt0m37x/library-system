package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import java.time.Instant

data class TransferRequestedEvent(
    override val id: TransferId,
    val sourceLibraryId: LibraryId,
    val destinationLibraryId: LibraryId,
    val bookId: BookId,
    val quantity: Int,
    val requestedBy: String,
    val requestedAt: Instant,
    val correlationId: String? = null,
    val causationId: String? = null
) : TransferEvent(id) {

    constructor(command: RequestTransferCommand) : this(
        id = command.id,
        sourceLibraryId = command.sourceLibraryId,
        destinationLibraryId = command.destinationLibraryId,
        bookId = command.bookId,
        quantity = command.quantity,
        requestedBy = command.requestedBy,
        requestedAt = requireNotNull(command.requestedAt) {
            "requestedAt must be set before publishing TransferRequestedEvent"
        },
        correlationId = command.correlationId,
        causationId = command.causationId
    )
}
