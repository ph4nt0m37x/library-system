package com.catalogservice.model.command

import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId
import org.axonframework.modelling.command.TargetAggregateIdentifier


data class CreateBookCommand(
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationYear: Int?,
    val category:  BookCategory?
)
