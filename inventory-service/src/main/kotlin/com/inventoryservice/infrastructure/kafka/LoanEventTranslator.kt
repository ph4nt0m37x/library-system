package com.inventoryservice.infrastructure.kafka

import com.inventoryservice.model.command.library.BorrowBookStockCommand
import com.inventoryservice.model.command.library.MarkBookStockLostCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.command.library.ReturnBookStockCommand
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.dto.LoanCreatedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedLostEventDTO
import com.inventoryservice.model.valueObject.dto.LoanReturnedEventDTO
import com.inventoryservice.model.command.library.MarkBookStockDamagedCommand
import com.inventoryservice.model.valueObject.dto.LoanMarkedDamagedEventDTO
import org.springframework.stereotype.Component

@Component
class LoanEventTranslator {

    fun translate(event: LoanCreatedEventDTO): BorrowBookStockCommand =
        BorrowBookStockCommand(
            libraryId = LibraryId(event.libraryId),
            bookId = BookId(event.bookId),
            quantity = 1,
            eventId = event.eventId,
            loanId = event.loanId,
            eventVersion = event.eventVersion,
            aggregateVersion = event.aggregateVersion,
            occurredAt = event.occurredAt,
            idempotencyKey = event.idempotencyKey
        )

    fun translate(event: LoanReturnedEventDTO): ReturnBookStockCommand =
        ReturnBookStockCommand(
            libraryId = LibraryId(event.libraryId),
            bookId = BookId(event.bookId),
            quantity = 1,
            eventId = event.eventId,
            loanId = event.loanId,
            eventVersion = event.eventVersion,
            aggregateVersion = event.aggregateVersion,
            occurredAt = event.occurredAt,
            idempotencyKey = event.idempotencyKey
        )

    fun translate(event: LoanMarkedLostEventDTO): MarkBookStockLostCommand =
        MarkBookStockLostCommand(
            libraryId = LibraryId(event.libraryId),
            bookId = BookId(event.bookId),
            quantity = 1,
            eventId = event.eventId,
            loanId = event.loanId,
            eventVersion = event.eventVersion,
            aggregateVersion = event.aggregateVersion,
            occurredAt = event.occurredAt,
            idempotencyKey = event.idempotencyKey
        )

    fun translate(event: LoanMarkedDamagedEventDTO): MarkBookStockDamagedCommand =
        MarkBookStockDamagedCommand(
            libraryId = LibraryId(event.libraryId),
            bookId = BookId(event.bookId),
            quantity = 1,
            eventId = event.eventId,
            loanId = event.loanId,
            eventVersion = event.eventVersion,
            aggregateVersion = event.aggregateVersion,
            occurredAt = event.occurredAt,
            idempotencyKey = event.idempotencyKey
        )
}
