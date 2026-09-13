package com.inventoryservice.service.impl

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.BorrowBookStockCommand
import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.MarkBookStockLostCommand
import com.inventoryservice.model.command.library.MarkBookStockDamagedCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.command.library.ReturnBookStockCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.dto.LibraryCreatedResponse
import com.inventoryservice.model.exception.DomainConflictException
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.valueObject.LibraryAddress
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.LibraryName
import com.inventoryservice.repository.LibraryRepository
import com.inventoryservice.service.LibraryService
import com.inventoryservice.service.CatalogBookPolicy
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class LibraryServiceImpl(
    private val commandGateway: CommandGateway,
    private val catalogBookPolicy: CatalogBookPolicy,
    private val libraryRepository: LibraryRepository
) : LibraryService {

    override fun createLibrary(
        command: CreateLibraryCommand
    ): CompletableFuture<LibraryCreatedResponse> {
        val name = LibraryName.of(command.name).value
        val address = LibraryAddress.of(command.address).value
        requireUniqueNameAndAddress(name, address)
        return commandGateway.send<Any?>(command)
            .thenApply { LibraryCreatedResponse(command.id.baseValue()) }
    }

    override fun updateLibrary(
        command: UpdateLibraryCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.id)
        val name = LibraryName.of(command.name).value
        val address = LibraryAddress.of(command.address).value
        requireUniqueNameAndAddress(name, address, command.id)
        return sendWithoutResult(command)
    }

    override fun deleteLibrary(
        command: DeleteLibraryCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.id)
        return sendWithoutResult(command)
    }

    override fun addBookStock(
        command: AddBookStockCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.libraryId)
        catalogBookPolicy.requireActive(command.bookId.baseValue())
        return sendWithoutResult(command)
    }

    override fun removeBookStock(
        command: RemoveBookStockCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.libraryId)
        return sendWithoutResult(command)
    }

    override fun borrowBookStock(
        command: BorrowBookStockCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.libraryId)
        return sendWithoutResult(command)
    }

    override fun returnBookStock(
        command: ReturnBookStockCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.libraryId)
        return sendWithoutResult(command)
    }

    override fun markBookStockLost(
        command: MarkBookStockLostCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.libraryId)
        return sendWithoutResult(command)
    }

    override fun markBookStockDamaged(
        command: MarkBookStockDamagedCommand
    ): CompletableFuture<Void> {
        validateLibrary(command.libraryId)
        return sendWithoutResult(command)
    }

    private fun sendWithoutResult(command: Any): CompletableFuture<Void> =
        commandGateway.send<Void>(command)

    private fun validateLibrary(id: LibraryId) {
        val library = libraryRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Library '${id.baseValue()}' does not exist")
        }

        if (library.isDeleted()) {
            throw DomainConflictException("Library '${id.baseValue()}' is deleted")
        }
    }

    private fun requireUniqueNameAndAddress(
        name: String,
        address: String,
        excludingId: LibraryId? = null
    ) {
        val duplicate = if (excludingId == null) {
            libraryRepository.existsByNameAndAddress(name, address)
        } else {
            libraryRepository.existsByNameAndAddressAndIdNot(name, address, excludingId)
        }

        if (duplicate) {
            throw DomainConflictException(
                "A library with name '$name' and address '$address' already exists"
            )
        }
    }
}
