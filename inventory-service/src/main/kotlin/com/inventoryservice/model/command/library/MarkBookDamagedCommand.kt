package com.inventoryservice.model.command.library

import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class MarkBookStockDamagedCommand(
    @TargetAggregateIdentifier
    val libraryId: LibraryId,
    val bookId: BookId,
    val quantity: Int,
    val eventId: String? = null,
    val loanId: String? = null,
    val eventVersion: Int? = null,
    val aggregateVersion: Long? = null,
    val occurredAt: Instant? = null,
    val idempotencyKey: String? = null
)
