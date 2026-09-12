package com.inventoryservice.model.valueObject.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateLibraryDTO(
    @field:NotBlank
    val id: String,
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    @field:NotBlank
    @field:Size(max = 500)
    val address: String
)
