package com.inventoryservice.model.valueObject.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class LoanCreatedEventDTO(
    val bookId: String,
    val libraryId: String
)