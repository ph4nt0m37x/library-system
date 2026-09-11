package com.inventoryservice.handler.eventHandler

import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.event.transfer.TransferCompletedEvent
import com.inventoryservice.model.event.transfer.TransferShippedEvent
import com.inventoryservice.service.LibraryService
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component

@Component
class TransferEventHandler(
    private val libraryService: LibraryService
) {

    @EventHandler
    fun handle(event: TransferShippedEvent) {
        libraryService.removeBookStock(
            RemoveBookStockCommand(
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