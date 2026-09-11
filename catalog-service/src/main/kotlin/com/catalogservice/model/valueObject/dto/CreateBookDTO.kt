package com.catalogservice.model.valueObject.dto

import java.math.BigDecimal

data class CreateBookDTO(
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationYear: Int?,
    val price: BigDecimal,
    val categoryId: Long?
)
