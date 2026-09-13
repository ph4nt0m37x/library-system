package com.inventoryservice.service

import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.view.LibraryView

interface LibraryViewReadService {

    fun findById(id: LibraryId): LibraryView?

    fun findAll(): List<LibraryView>

    fun findAllAvailable(): List<LibraryView>

}
