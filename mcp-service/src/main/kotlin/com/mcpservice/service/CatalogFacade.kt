package com.mcpservice.service

import com.mcpservice.client.CatalogClient
import com.mcpservice.client.dto.CatalogBookWire
import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import com.mcpservice.projection.BookProjection
import com.mcpservice.projection.CatalogSearchProjection
import com.mcpservice.projection.IdentifierNormalizer
import org.springframework.stereotype.Service

@Service
class CatalogFacade(
    private val catalogClient: CatalogClient,
    private val errors: DownstreamErrorTranslator
) {
    fun search(
        title: String?,
        author: String?,
        categoryId: Long?,
        limit: Int = DEFAULT_LIMIT
    ): CatalogSearchProjection {
        val normalizedTitle = title?.trim()?.takeIf(String::isNotEmpty)
        val normalizedAuthor = author?.trim()?.takeIf(String::isNotEmpty)
        requireLimit(limit)
        if (categoryId != null && categoryId <= 0) {
            throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, "categoryId must be greater than zero.")
        }

        val candidates = errors.call("Catalog service", ToolErrorCode.BOOK_NOT_FOUND) {
            selectCandidates(normalizedTitle, normalizedAuthor, categoryId)
        }
        val matches = candidates.asSequence()
            .filterNot { it.deleted }
            .filter { normalizedTitle == null || it.title.contains(normalizedTitle, ignoreCase = true) }
            .filter { normalizedAuthor == null || it.author.contains(normalizedAuthor, ignoreCase = true) }
            .filter { categoryId == null || it.category?.id == categoryId }
            .toList()
        val books = matches.take(limit).map { it.toProjection() }
        return CatalogSearchProjection(books, books.size, limit, matches.size > limit)
    }

    fun getBook(bookId: String): BookProjection {
        val id = IdentifierNormalizer.requireUuid(bookId, "bookId")
        val book = errors.call("Catalog service", ToolErrorCode.BOOK_NOT_FOUND) {
            catalogClient.findById(id)
        }
        if (book.deleted) {
            throw LibraryMcpToolException(ToolErrorCode.BOOK_NOT_FOUND, "The requested book was not found.")
        }
        val price = errors.call("Catalog service", ToolErrorCode.BOOK_NOT_FOUND) {
            catalogClient.getPrice(id)
        }
        return book.toProjection(price.currency)
    }

    private fun selectCandidates(title: String?, author: String?, categoryId: Long?): List<CatalogBookWire> = when {
        categoryId != null -> catalogClient.filterByCategory(categoryId)
        title != null -> catalogClient.searchByTitle(title)
        author != null -> catalogClient.searchByAuthor(author)
        else -> catalogClient.findAvailable()
    }

    private fun requireLimit(limit: Int) {
        if (limit !in 1..MAX_LIMIT) {
            throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, "limit must be between 1 and $MAX_LIMIT.")
        }
    }

    companion object {
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 50
    }
}
