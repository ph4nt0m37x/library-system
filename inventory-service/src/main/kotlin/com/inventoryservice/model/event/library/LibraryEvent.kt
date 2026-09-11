package com.inventoryservice.model.event.library

import com.inventoryservice.model.event.AbstractEvent
import com.inventoryservice.model.valueObject.LibraryId

abstract class LibraryEvent(
    open val id: LibraryId,
) : AbstractEvent(id)
