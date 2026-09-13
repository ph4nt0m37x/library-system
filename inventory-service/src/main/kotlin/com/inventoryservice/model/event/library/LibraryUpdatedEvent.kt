package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.valueObject.LibraryAddress
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.LibraryName

data class LibraryUpdatedEvent(
    override val id: LibraryId,
    val name: String,
    val address: String
) : LibraryEvent(id) {
    constructor(command: UpdateLibraryCommand) : this(
        id = command.id,
        name = LibraryName.of(command.name).value,
        address = LibraryAddress.of(command.address).value
    )
}
