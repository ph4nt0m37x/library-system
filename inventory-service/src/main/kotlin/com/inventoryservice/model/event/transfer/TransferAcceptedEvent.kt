package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import java.time.Instant

data class TransferAcceptedEvent(
    override val id: TransferId,
    val reviewedBy: String,
    val sourceLibraryId: LibraryId,
    val bookId: BookId,
    val quantity: Int,
    val reviewedAt: Instant
) : TransferEvent(id) {

    constructor(
        command: AcceptTransferCommand,
        sourceLibraryId: LibraryId,
        bookId: BookId,
        quantity: Int
    ) : this(
        id = command.id,
        reviewedBy = command.reviewedBy,
        sourceLibraryId = sourceLibraryId,
        bookId = bookId,
        quantity = quantity,
        reviewedAt = requireNotNull(command.reviewedAt) {
            "reviewedAt must be set before publishing TransferAcceptedEvent"
        }
    )
}
