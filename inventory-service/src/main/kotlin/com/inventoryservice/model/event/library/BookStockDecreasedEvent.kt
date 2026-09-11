package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.valueObject.LibraryId

data class BookStockDecreasedEvent(
    override val id: LibraryId,
    val bookId: String,
    val quantity: Int
) : LibraryEvent(id) {

    constructor(command: RemoveBookStockCommand) : this(
        id = command.libraryId,
        bookId = command.bookId,
        quantity = command.quantity
    )
}