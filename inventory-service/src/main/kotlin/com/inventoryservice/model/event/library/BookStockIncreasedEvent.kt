package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.valueObject.LibraryId

data class BookStockIncreasedEvent(
    override val id: LibraryId,
    val bookId: String,
    val quantity: Int
) : LibraryEvent(id) {

    constructor(command: AddBookStockCommand) : this(
        id = command.libraryId,
        bookId = command.bookId,
        quantity = command.quantity
    )
}