package com.catalogservice.model.command

import com.catalogservice.model.valueObject.BookId
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class DeleteBookCommand(
    @TargetAggregateIdentifier
    val id: BookId
)
