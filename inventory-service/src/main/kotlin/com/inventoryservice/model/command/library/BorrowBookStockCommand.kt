package com.inventoryservice.model.command.library

import com.inventoryservice.model.valueObject.LibraryId
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class BorrowBookStockCommand(
    @TargetAggregateIdentifier
    val libraryId: LibraryId,
    val bookId: String,
    val quantity: Int
)