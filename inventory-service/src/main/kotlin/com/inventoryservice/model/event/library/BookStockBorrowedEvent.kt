package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.BorrowBookStockCommand
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import java.time.Instant

data class BookStockBorrowedEvent(
    override val id: LibraryId,
    val bookId: BookId,
    val quantity: Int,
    val eventId: String? = null,
    val loanId: String? = null,
    val eventVersion: Int? = null,
    val occurredAt: Instant? = null,
    val idempotencyKey: String? = null
) : LibraryEvent(id) {

    constructor(command: BorrowBookStockCommand) : this(
        id = command.libraryId,
        bookId = command.bookId,
        quantity = command.quantity,
        eventId = command.eventId,
        loanId = command.loanId,
        eventVersion = command.eventVersion,
        occurredAt = command.occurredAt,
        idempotencyKey = command.idempotencyKey
    )
}
