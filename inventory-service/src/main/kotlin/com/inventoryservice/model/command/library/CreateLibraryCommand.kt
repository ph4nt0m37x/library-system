package com.inventoryservice.model.command.library

import com.inventoryservice.model.valueObject.LibraryId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.axonframework.modelling.command.TargetAggregateIdentifier

data class CreateLibraryCommand(
    @TargetAggregateIdentifier
    val id: LibraryId = LibraryId(),
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    @field:NotBlank
    @field:Size(max = 500)
    val address: String
)
