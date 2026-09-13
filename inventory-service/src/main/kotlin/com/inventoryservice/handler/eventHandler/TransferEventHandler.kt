package com.inventoryservice.handler.eventHandler

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.event.transfer.TransferAcceptedEvent
import com.inventoryservice.model.event.transfer.TransferCancelledEvent
import com.inventoryservice.model.event.transfer.TransferCompletedEvent
import com.inventoryservice.service.LibraryService
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("transfer-stock")
class TransferEventHandler(
    private val libraryService: LibraryService
) {

    @EventHandler
    fun handle(event: TransferAcceptedEvent) {
        libraryService.removeBookStock(
            RemoveBookStockCommand(
                libraryId = event.sourceLibraryId,
                bookId = event.bookId,
                quantity = event.quantity
            )
        ).join()
    }

    @EventHandler
    fun handle(event: TransferCancelledEvent) {
        if (!event.releaseStock) {
            return
        }

        libraryService.addBookStock(
            AddBookStockCommand(
                libraryId = event.sourceLibraryId,
                bookId = event.bookId,
                quantity = event.quantity
            )
        ).join()
    }

    @EventHandler
    fun handle(event: TransferCompletedEvent) {
        libraryService.addBookStock(
            AddBookStockCommand(
                libraryId = event.destinationLibraryId,
                bookId = event.bookId,
                quantity = event.quantity
            )
        ).join()
    }
}
