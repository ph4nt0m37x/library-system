package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.BorrowBookStockCommand
import com.inventoryservice.model.valueObject.LibraryId

data class BookStockBorrowedEvent(
    override val id: LibraryId,
    val bookId: String,
    val quantity: Int
) : LibraryEvent(id) {

    constructor(command: BorrowBookStockCommand) : this(
        id = command.libraryId,
        bookId = command.bookId,
        quantity = command.quantity
    )
}