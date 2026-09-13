package com.mcpservice.client.dto

import tools.jackson.databind.JsonNode

data class LibraryWire(
    val id: JsonNode,
    val name: String,
    val address: String,
    val deleted: Boolean = false
)

data class BookStockWire(
    val id: Long? = null,
    val libraryId: String,
    val bookId: JsonNode,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val borrowedQuantity: Int? = null
)

data class StockAvailabilityWire(
    val libraryId: String,
    val bookId: String,
    val libraryActive: Boolean,
    val bookActive: Boolean,
    val availableQuantity: Int,
    val available: Boolean
)
