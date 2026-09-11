package com.catalogservice.model.event

import com.catalogservice.model.valueObject.BookId

data class BookDeletedExternalEvent(
    val bookId: BookId
)