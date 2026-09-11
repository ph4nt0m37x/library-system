package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.valueObject.LibraryId

data class LibraryUpdatedEvent(
    override val id: LibraryId,
    val name: String,
    val address: String
) : LibraryEvent(id) {
    constructor(command: UpdateLibraryCommand) : this(
        id = LibraryId(),
        name = command.name,
        address = command.address
    )
}