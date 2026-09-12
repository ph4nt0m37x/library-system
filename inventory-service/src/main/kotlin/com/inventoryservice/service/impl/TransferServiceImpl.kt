package com.inventoryservice.service.impl

import com.inventoryservice.model.aggregate.Transfer
import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.exception.DomainConflictException
import com.inventoryservice.model.exception.DomainValidationException
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.model.valueObject.TransferStatus
import com.inventoryservice.repository.BookStockRepository
import com.inventoryservice.repository.LibraryRepository
import com.inventoryservice.repository.TransferRepository
import com.inventoryservice.service.TransferService
import com.inventoryservice.service.CatalogBookPolicy
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture

@Service
class TransferServiceImpl(
    private val commandGateway: CommandGateway,
    private val libraryRepository: LibraryRepository,
    private val transferRepository: TransferRepository,
    private val bookStockRepository: BookStockRepository,
    private val catalogBookPolicy: CatalogBookPolicy,
    private val clock: Clock
) : TransferService {

    override fun requestTransfer(
        command: RequestTransferCommand
    ): CompletableFuture<TransferId> {
        BookStock.validateChangeQuantity(command.quantity)
        if (command.sourceLibraryId == command.destinationLibraryId) {
            throw DomainValidationException("Source and destination libraries must be different")
        }

        validateLibraries(command.sourceLibraryId, command.destinationLibraryId)
        validateBook(command.bookId)
        validateSourceStock(command.sourceLibraryId, command.bookId, command.quantity)

        val timestampedCommand = command.copy(requestedAt = Instant.now(clock))
        return commandGateway.send<Any?>(timestampedCommand)
            .thenApply { timestampedCommand.id }
    }

    override fun acceptTransfer(
        command: AcceptTransferCommand
    ): CompletableFuture<Void> {
        val transfer = findTransfer(command.id)
        val timestampedCommand = command.copy(reviewedAt = Instant.now(clock))

        if (transfer.status() != TransferStatus.REQUESTED) {
            return sendWithoutResult(timestampedCommand)
        }

        return try {
            validateTransfer(transfer, validateStock = true)
            sendWithoutResult(timestampedCommand)
        } catch (exception: IllegalArgumentException) {
            commandGateway.send<Any?>(
                RejectTransferCommand(
                    id = command.id,
                    reviewedBy = command.reviewedBy,
                    reviewedAt = timestampedCommand.reviewedAt,
                    reason = exception.message
                )
            ).thenCompose {
                CompletableFuture.failedFuture<Void>(exception)
            }
        }
    }

    override fun rejectTransfer(
        command: RejectTransferCommand
    ): CompletableFuture<Void> =
        sendWithoutResult(command.copy(reviewedAt = Instant.now(clock)))

    override fun cancelTransfer(
        command: CancelTransferCommand
    ): CompletableFuture<Void> =
        sendWithoutResult(command)

    override fun shipTransfer(
        command: ShipTransferCommand
    ): CompletableFuture<Void> {
        validateTransfer(findTransfer(command.id), validateStock = false)
        return sendWithoutResult(command)
    }

    override fun completeTransfer(
        command: CompleteTransferCommand
    ): CompletableFuture<Void> {
        validateTransfer(findTransfer(command.id), validateStock = false)
        return sendWithoutResult(command.copy(completedAt = Instant.now(clock)))
    }

    private fun sendWithoutResult(command: Any): CompletableFuture<Void> =
        commandGateway.send<Void>(command)

    private fun validateTransfer(transfer: Transfer, validateStock: Boolean) {
        validateLibraries(transfer.sourceLibraryId(), transfer.destinationLibraryId())
        validateBook(transfer.bookId())

        if (validateStock) {
            validateSourceStock(
                transfer.sourceLibraryId(),
                transfer.bookId(),
                transfer.quantity()
            )
        }
    }

    private fun validateLibraries(sourceLibraryId: LibraryId, destinationLibraryId: LibraryId) {
        validateLibrary(sourceLibraryId, "Source")
        validateLibrary(destinationLibraryId, "Destination")
    }

    private fun validateLibrary(libraryId: LibraryId, role: String) {
        val library = libraryRepository.findById(libraryId).orElseThrow {
            ResourceNotFoundException("$role library '${libraryId.baseValue()}' does not exist")
        }

        if (library.isDeleted()) {
            throw DomainConflictException("$role library '${libraryId.baseValue()}' is deleted")
        }
    }

    private fun validateBook(bookId: BookId) {
        catalogBookPolicy.requireActive(bookId.baseValue())
    }

    private fun validateSourceStock(libraryId: LibraryId, bookId: BookId, quantity: Int) {
        val stock = bookStockRepository.findByLibraryIdAndBookId(libraryId.value, bookId)

        if (stock == null) {
            throw ResourceNotFoundException(
                "Book '${bookId.baseValue()}' is not stocked in the source library"
            )
        }
        if (stock.availableQuantity < quantity) {
            throw DomainConflictException(
                "Source library does not have $quantity available copies of book '${bookId.baseValue()}'"
            )
        }
    }

    private fun findTransfer(id: TransferId): Transfer =
        transferRepository.findById(id).orElseThrow {
            ResourceNotFoundException("Transfer '${id.baseValue()}' does not exist")
        }
}
