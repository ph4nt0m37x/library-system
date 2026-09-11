package com.inventoryservice.web

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.service.LibraryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stock")
class LibraryStockRestApi(private val libraryService: LibraryService) {
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
}