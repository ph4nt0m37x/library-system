package com.mcpservice.client.dto

import tools.jackson.databind.JsonNode
import java.math.BigDecimal

data class CatalogBookWire(
    val id: JsonNode,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String? = null,
    val publicationYear: Int? = null,
    val price: MoneyWire,
    val category: CategoryWire? = null,
    val deleted: Boolean = false
)

data class MoneyWire(val amount: BigDecimal)

data class CategoryWire(
    val id: Long? = null,
    val name: String
)

data class BookPriceWire(
    val amount: BigDecimal,
    val currency: String
)
