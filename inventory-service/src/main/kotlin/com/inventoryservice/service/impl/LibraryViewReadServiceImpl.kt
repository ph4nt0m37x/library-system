package com.inventoryservice.service.impl

import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.view.LibraryView
import com.inventoryservice.repository.LibraryViewReadRepository
import com.inventoryservice.service.LibraryViewReadService
import org.springframework.stereotype.Service

@Service
class LibraryViewReadServiceImpl(
    private val libraryViewReadRepository: LibraryViewReadRepository
) : LibraryViewReadService {

    override fun findById(id: LibraryId): LibraryView? =
        libraryViewReadRepository.findById(id).orElse(null)

    override fun findAll(): List<LibraryView> =
        libraryViewReadRepository.findAll()

}