package com.inventoryservice.model.command.library

import org.axonframework.modelling.command.TargetAggregateIdentifier
import com.inventoryservice.model.valueObject.LibraryId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateLibraryCommand(
    @TargetAggregateIdentifier
    val id: LibraryId,
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    @field:NotBlank
    @field:Size(max = 500)
    val address: String
)
