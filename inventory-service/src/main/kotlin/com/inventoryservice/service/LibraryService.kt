package com.inventoryservice.service


import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.valueObject.LibraryId
import java.util.concurrent.CompletableFuture

interface LibraryService {

    fun createLibrary(command: CreateLibraryCommand): CompletableFuture<LibraryId>

    fun updateLibrary(command: UpdateLibraryCommand): CompletableFuture<LibraryId>

    fun deleteLibrary(command: DeleteLibraryCommand): CompletableFuture<LibraryId>

    fun addBookStock(command: AddBookStockCommand): CompletableFuture<LibraryId>

    fun removeBookStock(command: RemoveBookStockCommand): CompletableFuture<LibraryId>
}