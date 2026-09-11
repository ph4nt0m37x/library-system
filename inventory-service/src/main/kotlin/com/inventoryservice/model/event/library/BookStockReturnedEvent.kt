package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.ReturnBookStockCommand
import com.inventoryservice.model.valueObject.LibraryId

data class BookStockReturnedEvent(
    override val id: LibraryId,
    val bookId: String,
    val quantity: Int
) : LibraryEvent(id) {

    constructor(command: ReturnBookStockCommand) : this(
        id = command.libraryId,
        bookId = command.bookId,
        quantity = command.quantity
    )
}