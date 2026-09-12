package com.inventoryservice.model.valueObject.dto

import jakarta.validation.constraints.NotBlank

data class DeleteLibraryDTO(
    @field:NotBlank
    val id: String
)
