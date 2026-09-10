package com.catalogservice.model.event

import com.catalogservice.model.command.DeleteBookCommand
import com.catalogservice.model.valueObject.BookId

data class BookDeletedEvent(
    override val id: BookId
) : BookEvent(id) {

    constructor(command: DeleteBookCommand) : this(
        id = command.id
    )
}

