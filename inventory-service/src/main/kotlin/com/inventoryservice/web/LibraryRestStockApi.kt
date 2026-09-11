package com.inventoryservice.web

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.service.BookStockReadService
import com.inventoryservice.service.LibraryService
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
        @RequestBody command: AddBookStockCommand
    ): ResponseEntity<LibraryId> =
        ResponseEntity.ok(
            libraryService.addBookStock(command).join()
        )

    @DeleteMapping
    fun removeBookStock(
        @RequestBody command: RemoveBookStockCommand
    ): ResponseEntity<LibraryId> =
        ResponseEntity.ok(
            libraryService.removeBookStock(command).join()
        )

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
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(stock)
    }

    @GetMapping("/book/{bookId}")
    fun getBookStockAcrossLibraries(
        @PathVariable bookId: String
    ): ResponseEntity<List<BookStock>> =
        ResponseEntity.ok(
            bookStockReadService.findByBook(bookId)
        )
}