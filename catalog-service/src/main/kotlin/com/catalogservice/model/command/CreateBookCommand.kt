package com.catalogservice.model.command

import com.catalogservice.model.valueObject.BookCategory
import com.catalogservice.model.valueObject.BookId
import org.axonframework.modelling.command.TargetAggregateIdentifier


data class CreateBookCommand(
    @TargetAggregateIdentifier
    val id: BookId,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationDate: String?,
    val category:  BookCategory?
)
