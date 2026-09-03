package com.catalogservice.model.event

import com.catalogservice.model.valueObject.BookCategory
import com.catalogservice.model.valueObject.BookId

data class BookCreatedEvent(

    val id: BookId,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publisher: String?,
    val publicationDate: String?,
    val category:  BookCategory?
)

