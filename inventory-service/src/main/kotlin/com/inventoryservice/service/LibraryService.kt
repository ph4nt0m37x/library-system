package com.inventoryservice.service


import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.BorrowBookStockCommand
import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.MarkBookStockLostCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.command.library.ReturnBookStockCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.dto.LibraryCreatedResponse
import java.util.concurrent.CompletableFuture

interface LibraryService {

    fun createLibrary(command: CreateLibraryCommand): CompletableFuture<LibraryCreatedResponse>

    fun updateLibrary(command: UpdateLibraryCommand): CompletableFuture<Void>

    fun deleteLibrary(command: DeleteLibraryCommand): CompletableFuture<Void>

    fun addBookStock(command: AddBookStockCommand): CompletableFuture<Void>

    fun removeBookStock(command: RemoveBookStockCommand): CompletableFuture<Void>

    fun borrowBookStock(command: BorrowBookStockCommand): CompletableFuture<Void>

    fun returnBookStock(command: ReturnBookStockCommand): CompletableFuture<Void>

    fun markBookStockLost(command: MarkBookStockLostCommand): CompletableFuture<Void>
}
