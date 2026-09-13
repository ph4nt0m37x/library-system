package com.mcpservice.mcp

import com.mcpservice.projection.BookProjection
import com.mcpservice.projection.CatalogSearchProjection
import com.mcpservice.service.CatalogFacade
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class CatalogTools(
    private val catalogFacade: CatalogFacade
) {
    @PreAuthorize("hasRole('library-user')")
    @McpTool(
        name = "catalog_search",
        title = "Search the library catalog",
        description = "Search non-retired library books by optional title, author, and category. Multiple filters use AND semantics. With no filters, returns available catalog books. Results are capped by limit.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Search the library catalog",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun search(
        @McpToolParam(description = "Case-insensitive title fragment", required = false)
        title: String?,
        @McpToolParam(description = "Case-insensitive author fragment", required = false)
        author: String?,
        @McpToolParam(description = "Positive catalog category identifier", required = false)
        categoryId: Long?,
        @McpToolParam(description = "Maximum results, from 1 to 50; defaults to 10", required = false)
        limit: Int?
    ): CatalogSearchProjection = catalogFacade.search(
        title = McpArguments.optionalSearch(title, "title"),
        author = McpArguments.optionalSearch(author, "author"),
        categoryId = McpArguments.categoryId(categoryId),
        limit = McpArguments.limit(limit)
    )

    @PreAuthorize("hasRole('library-user')")
    @McpTool(
        name = "catalog_get_book",
        title = "Get catalog book details",
        description = "Get one non-retired catalog book by its identifier, including an explicit price amount and currency. Use catalog_search first when the identifier is unknown.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Get catalog book details",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun getBook(
        @McpToolParam(description = "Book identifier returned by catalog_search", required = true)
        bookId: String
    ): BookProjection = catalogFacade.getBook(McpArguments.identifier(bookId, "bookId"))
}
