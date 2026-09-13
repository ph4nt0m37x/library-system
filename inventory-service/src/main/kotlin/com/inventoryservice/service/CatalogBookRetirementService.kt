package com.inventoryservice.service

import com.inventoryservice.model.entity.CatalogBookReference
import com.inventoryservice.model.valueObject.dto.BookDeletedIntegrationEventDTO
import com.inventoryservice.repository.CatalogBookReferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CatalogBookRetirementService(
    private val catalogBookReferenceRepository: CatalogBookReferenceRepository
) {
    @Transactional
    fun process(event: BookDeletedIntegrationEventDTO): Boolean {
        event.validate()
        val existing = catalogBookReferenceRepository.findById(event.bookId).orElse(null)
        if (existing != null) {
            if (existing.eventId == event.eventId || existing.catalogAggregateVersion >= event.aggregateVersion) {
                return false
            }
            throw IllegalStateException("Book ${event.bookId} already has a different retirement event")
        }

        catalogBookReferenceRepository.save(
            CatalogBookReference(
                bookId = event.bookId,
                retired = true,
                retiredAt = event.occurredAt,
                catalogAggregateVersion = event.aggregateVersion,
                eventId = event.eventId
            )
        )
        return true
    }
}
