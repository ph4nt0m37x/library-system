package com.mcpservice.projection

data class InventoryLocationProjection(
    val libraryId: String,
    val libraryName: String?,
    val address: String?,
    val totalQuantity: Int?,
    val availableQuantity: Int,
    val borrowedQuantity: Int?,
    val libraryActive: Boolean,
    val bookActive: Boolean,
    val available: Boolean
)

data class InventoryAvailabilityProjection(
    val bookId: String,
    val locations: List<InventoryLocationProjection>,
    val count: Int,
    val limit: Int,
    val truncated: Boolean
)
