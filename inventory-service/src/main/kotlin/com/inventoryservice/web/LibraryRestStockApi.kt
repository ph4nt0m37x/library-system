package com.inventoryservice.web

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.service.BookStockReadService
import com.inventoryservice.service.LibraryService
import com.inventoryservice.model.valueObject.dto.StockAvailabilityResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stock")
class LibraryStockRestApi(
    private val libraryService: LibraryService,
    private val bookStockReadService: BookStockReadService
) {

    @PostMapping
    fun addBookStock(
        @Valid @RequestBody command: AddBookStockCommand
    ): ResponseEntity<Void> {
        libraryService.addBookStock(command).join()
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping
    fun removeBookStock(
        @Valid @RequestBody command: RemoveBookStockCommand
    ): ResponseEntity<Void> {
        libraryService.removeBookStock(command).join()
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{libraryId}")
    fun getLibraryStock(
        @PathVariable libraryId: String
    ): ResponseEntity<List<BookStock>> =
        ResponseEntity.ok(
            bookStockReadService.findByLibrary(libraryId)
        )

    @GetMapping("/{libraryId}/{bookId}")
    fun getBookStock(
        @PathVariable libraryId: String,
        @PathVariable bookId: String
    ): ResponseEntity<BookStock> {
        val stock = bookStockReadService.findByLibraryAndBook(
            libraryId,
            bookId
        ) ?: throw ResourceNotFoundException(
            "Book '$bookId' is not stocked in library '$libraryId'"
        )

        return ResponseEntity.ok(stock)
    }

    @GetMapping("/{libraryId}/{bookId}/availability")
    fun getAvailability(
        @PathVariable libraryId: String,
        @PathVariable bookId: String
    ): ResponseEntity<StockAvailabilityResponse> =
        ResponseEntity.ok(bookStockReadService.availability(libraryId, bookId))

    @GetMapping("/book/{bookId}")
    fun getBookStockAcrossLibraries(
        @PathVariable bookId: String
    ): ResponseEntity<List<BookStock>> =
        ResponseEntity.ok(
            bookStockReadService.findByBook(bookId)
        )
}
