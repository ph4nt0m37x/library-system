package com.catalogservice.model.valueObject.dto

import com.catalogservice.model.entity.BookCategory
import java.math.BigDecimal

data class CreateBookDTO(
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publisher: String?,
    val publicationYear: Int?,
    val price: BigDecimal,
    val category: BookCategory?
)
