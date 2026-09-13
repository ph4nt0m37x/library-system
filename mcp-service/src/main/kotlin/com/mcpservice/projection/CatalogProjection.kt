package com.mcpservice.projection

import java.math.BigDecimal

data class MoneyProjection(
    val amount: BigDecimal,
    // Search responses do not carry a currency; detail responses enrich it via /price.
    val currency: String?
)

data class CategoryProjection(
    val categoryId: Long?,
    val name: String
)

data class BookProjection(
    val bookId: String,
    val isbn: String,
    val title: String,
    val author: String,
    val description: String?,
    val publicationYear: Int?,
    val price: MoneyProjection,
    val category: CategoryProjection?
)

data class CatalogSearchProjection(
    val books: List<BookProjection>,
    val count: Int,
    val limit: Int,
    val truncated: Boolean
)
