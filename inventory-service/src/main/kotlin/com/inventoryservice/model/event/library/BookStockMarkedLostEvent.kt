package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.MarkBookStockLostCommand
import com.inventoryservice.model.valueObject.LibraryId

data class BookStockMarkedLostEvent(
    override val id: LibraryId,
    val bookId: String,
    val quantity: Int
) : LibraryEvent(id) {

    constructor(command: MarkBookStockLostCommand) : this(
        id = command.libraryId,
        bookId = command.bookId,
        quantity = command.quantity
    )
}