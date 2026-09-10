package com.catalogservice.model.event

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId

data class BookCreatedEvent(
    override val id: BookId,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationYear: Int?,
    val category: BookCategory?
) : BookEvent(id) {

    constructor(command: CreateBookCommand) : this(
        id = BookId(),
        isbn = command.isbn,
        title = command.title,
        author = command.author,
        description = command.description,
        publicationYear = command.publicationYear,
        category = command.category
    )

//    override fun toExternalEvent(): BookCreatedExternalEvent {
//        return BookCreatedExternalEvent(
//            bookId = this.bookId,
//            isbn = this.isbn,
//            title = this.title,
//            author = this.author,
//            description = this.description,
//            publicationYear = this.publicationYear,
//            category = this.category
//        )
//    }
}