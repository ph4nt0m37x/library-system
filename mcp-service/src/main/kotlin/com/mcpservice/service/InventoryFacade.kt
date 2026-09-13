package com.mcpservice.service

import com.mcpservice.client.InventoryClient
import com.mcpservice.error.ToolErrorCode
import com.mcpservice.projection.IdentifierNormalizer
import com.mcpservice.projection.InventoryAvailabilityProjection
import com.mcpservice.projection.InventoryLocationProjection
import org.springframework.stereotype.Service

@Service
class InventoryFacade(
    private val inventoryClient: InventoryClient,
    private val errors: DownstreamErrorTranslator
) {
    fun findCopies(bookId: String, libraryId: String? = null): InventoryAvailabilityProjection {
        val normalizedBookId = IdentifierNormalizer.requireUuid(bookId, "bookId")
        val normalizedLibraryId = libraryId?.let { IdentifierNormalizer.requireUuid(it, "libraryId") }
        if (normalizedLibraryId != null) {
            val stock = errors.call("Inventory service", ToolErrorCode.LIBRARY_NOT_FOUND) {
                inventoryClient.getAvailability(normalizedLibraryId, normalizedBookId)
            }
            val location = InventoryLocationProjection(
                libraryId = IdentifierNormalizer.base(stock.libraryId),
                libraryName = null,
                address = null,
                totalQuantity = null,
                availableQuantity = stock.availableQuantity,
                borrowedQuantity = null,
                libraryActive = stock.libraryActive,
                bookActive = stock.bookActive,
                available = stock.available
            )
            return InventoryAvailabilityProjection(normalizedBookId, listOf(location), 1, MAX_LOCATIONS, false)
        }

        val allStock = errors.call("Inventory service", ToolErrorCode.BOOK_NOT_FOUND) {
            inventoryClient.findAcrossLibraries(normalizedBookId)
        }.asSequence()
            .filter { it.availableQuantity > 0 }
            .distinctBy { IdentifierNormalizer.base(it.libraryId) }
            .toList()
        val selected = allStock.take(MAX_LOCATIONS)
        val locations = selected.map { stock ->
            val id = IdentifierNormalizer.base(stock.libraryId)
            val library = errors.call("Inventory service", ToolErrorCode.LIBRARY_NOT_FOUND) {
                inventoryClient.findLibrary(id)
            }
            InventoryLocationProjection(
                libraryId = id,
                libraryName = library.name,
                address = library.address,
                totalQuantity = stock.totalQuantity,
                availableQuantity = stock.availableQuantity,
                borrowedQuantity = stock.borrowedQuantity ?: (stock.totalQuantity - stock.availableQuantity),
                libraryActive = !library.deleted,
                bookActive = true,
                available = !library.deleted && stock.availableQuantity > 0
            )
        }
        return InventoryAvailabilityProjection(
            bookId = normalizedBookId,
            locations = locations,
            count = locations.size,
            limit = MAX_LOCATIONS,
            truncated = allStock.size > MAX_LOCATIONS
        )
    }

    companion object {
        const val MAX_LOCATIONS = 50
    }
}
