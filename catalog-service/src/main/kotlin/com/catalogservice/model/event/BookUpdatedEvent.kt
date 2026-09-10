package com.catalogservice.model.event

import com.catalogservice.model.command.UpdateBookCommand
import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId

data class BookUpdatedEvent(
    override val id: BookId,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationYear: Int?,
    val category: BookCategory?
) : BookEvent(id) {

    constructor(command: UpdateBookCommand) : this(
        id = command.id,
        isbn = command.isbn,
        title = command.title,
        author = command.author,
        description = command.description,
        publicationYear = command.publicationYear,
        category = command.category
    )
}

