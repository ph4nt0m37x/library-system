package com.catalogservice.model.command

import org.axonframework.modelling.command.TargetAggregateIdentifier

data class DeleteBookCommand(
    @TargetAggregateIdentifier
    val id: BookId
)
