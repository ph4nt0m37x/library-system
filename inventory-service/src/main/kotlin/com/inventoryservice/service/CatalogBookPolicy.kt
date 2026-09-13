package com.inventoryservice.service

import com.inventoryservice.client.CatalogBookClient
import com.inventoryservice.model.exception.DependencyUnavailableException
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.repository.CatalogBookReferenceRepository
import org.springframework.stereotype.Service

@Service
class CatalogBookPolicy(
    private val catalogBookClient: CatalogBookClient,
    private val catalogBookReferenceRepository: CatalogBookReferenceRepository
) {
    fun isActive(bookId: String): Boolean {
        if (catalogBookReferenceRepository.findById(bookId).map { it.retired }.orElse(false)) {
            return false
        }
        return try {
            catalogBookClient.isBookAvailable(bookId)
        } catch (exception: Exception) {
            throw DependencyUnavailableException("Catalog", exception)
        }
    }

    fun requireActive(bookId: String) {
        if (!isActive(bookId)) {
            throw ResourceNotFoundException("Book '$bookId' does not exist or is deleted in Catalog")
        }
    }
}
