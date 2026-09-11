package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.valueObject.LibraryId

data class LibraryCreatedEvent(
    override val id: LibraryId,
    val name: String,
    val address: String
) : LibraryEvent(id) {
    constructor(command: CreateLibraryCommand) : this(
        id = LibraryId(),
        name = command.name,
        address = command.address
    )
}
