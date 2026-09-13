package com.catalogservice.model.event

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.valueObject.Money

data class BookCreatedEvent(
    override val id: BookId,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationYear: Int?,
    val price: Money,
    val category: BookCategory?
) : BookEvent(id) {

    constructor(command: CreateBookCommand) : this(
        id = BookId(),
        isbn = command.isbn,
        title = command.title,
        author = command.author,
        description = command.description,
        publicationYear = command.publicationYear,
        price = command.price,
        category = command.category
    )

}
