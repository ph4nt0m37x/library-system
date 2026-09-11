package com.inventoryservice.model.event.library

import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.valueObject.LibraryId

data class LibraryDeletedEvent(
    override val id: LibraryId
): LibraryEvent(id) {

    constructor(command: DeleteLibraryCommand) : this(
        id = command.id
    )
}