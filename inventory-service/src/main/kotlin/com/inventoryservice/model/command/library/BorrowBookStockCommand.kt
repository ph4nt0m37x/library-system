package com.inventoryservice.model.command.library

import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.BookId
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class BorrowBookStockCommand(
    @TargetAggregateIdentifier
    val libraryId: LibraryId,
    val bookId: BookId,
    @field:Min(1)
    @field:Max(1_000_000)
    val quantity: Int,
    val eventId: String? = null,
    val loanId: String? = null,
    val eventVersion: Int? = null,
    val aggregateVersion: Long? = null,
    val occurredAt: Instant? = null,
    val idempotencyKey: String? = null
)
