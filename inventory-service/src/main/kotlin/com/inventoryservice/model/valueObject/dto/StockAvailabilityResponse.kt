package com.inventoryservice.model.valueObject.dto

data class StockAvailabilityResponse(
    val libraryId: String,
    val bookId: String,
    val libraryActive: Boolean,
    val bookActive: Boolean,
    val availableQuantity: Int,
    val available: Boolean
)
