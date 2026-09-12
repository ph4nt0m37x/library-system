package com.inventoryservice.model.command.transfer

import com.fasterxml.jackson.annotation.JsonIgnore
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.TransferId
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class RequestTransferCommand(
    @TargetAggregateIdentifier
    @JsonIgnore
    val id: TransferId = TransferId(),
    val sourceLibraryId: LibraryId,
    val destinationLibraryId: LibraryId,
    val bookId: BookId,
    @field:Min(1)
    @field:Max(1_000_000)
    val quantity: Int,
    @field:NotBlank
    @field:Size(max = 200)
    val requestedBy: String,
    val requestedAt: Instant? = null,
    @field:Size(max = 100)
    val correlationId: String? = null,
    @field:Size(max = 100)
    val causationId: String? = null
)
