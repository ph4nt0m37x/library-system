package com.inventoryservice.service.impl


import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.service.LibraryService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class LibraryServiceImpl(
    private val commandGateway: CommandGateway
) : LibraryService {

    override fun createLibrary(
        command: CreateLibraryCommand
    ): CompletableFuture<LibraryId> =
        commandGateway.send(command)

    override fun updateLibrary(
        command: UpdateLibraryCommand
    ): CompletableFuture<LibraryId> =
        commandGateway.send(command)

    override fun deleteLibrary(
        command: DeleteLibraryCommand
    ): CompletableFuture<LibraryId> =
        commandGateway.send(command)

    override fun addBookStock(
        command: AddBookStockCommand
    ): CompletableFuture<LibraryId> =
        commandGateway.send(command)

    override fun removeBookStock(
        command: RemoveBookStockCommand
    ): CompletableFuture<LibraryId> =
        commandGateway.send(command)
}