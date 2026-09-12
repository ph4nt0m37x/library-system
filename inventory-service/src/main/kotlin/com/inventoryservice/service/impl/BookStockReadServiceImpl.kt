package com.inventoryservice.service.impl

import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.repository.BookStockRepository
import com.inventoryservice.repository.LibraryRepository
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.dto.StockAvailabilityResponse
import com.inventoryservice.service.CatalogBookPolicy
import org.springframework.stereotype.Service
import com.inventoryservice.service.BookStockReadService

@Service
class BookStockReadServiceImpl(
    private val bookStockRepository: BookStockRepository,
    private val libraryRepository: LibraryRepository,
    private val catalogBookPolicy: CatalogBookPolicy
) : BookStockReadService {

    override fun findByLibrary(libraryId: String): List<BookStock> {
        return bookStockRepository.findByLibraryId(libraryId)
    }

    override fun findByLibraryAndBook(
        libraryId: String,
        bookId: String
    ): BookStock? {
        return bookStockRepository.findByLibraryIdAndBookId(
            libraryId,
            BookId(bookId)
        )
    }

    override fun findByBook(bookId: String): List<BookStock> {
        return bookStockRepository.findByBookId(BookId(bookId))
    }

    override fun availability(libraryId: String, bookId: String): StockAvailabilityResponse {
        val normalizedLibraryId = requireId(libraryId, "libraryId")
        val normalizedBookId = requireId(bookId, "bookId")
        val library = libraryRepository.findById(LibraryId(normalizedLibraryId)).orElseThrow {
            ResourceNotFoundException("Library '$normalizedLibraryId' does not exist")
        }
        val libraryActive = !library.isDeleted()
        val catalogActive = catalogBookPolicy.isActive(normalizedBookId)
        val stock = bookStockRepository.findByLibraryIdAndBookId(
            LibraryId(normalizedLibraryId).value,
            BookId(normalizedBookId)
        )
        val availableQuantity = stock?.availableQuantity ?: 0

        return StockAvailabilityResponse(
            libraryId = normalizedLibraryId,
            bookId = normalizedBookId,
            libraryActive = libraryActive,
            bookActive = catalogActive,
            availableQuantity = availableQuantity,
            available = libraryActive && catalogActive && availableQuantity > 0
        )
    }

    private fun requireId(value: String, field: String): String {
        val normalized = value.trim()
        require(normalized.isNotEmpty()) { "$field must not be blank" }
        require(normalized.length <= 100) { "$field must not exceed 100 characters" }
        return normalized
    }
}
