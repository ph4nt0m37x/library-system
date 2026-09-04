package com.catalogservice.model.valueObject.dto

import com.catalogservice.model.entity.BookCategory

data class CreateBookDTO(
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publisher: String?,
    val publicationYear: Int?,
    val category: BookCategory?
)
