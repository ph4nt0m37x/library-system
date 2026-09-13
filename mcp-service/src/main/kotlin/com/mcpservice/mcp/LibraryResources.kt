package com.mcpservice.mcp

import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import com.mcpservice.service.CatalogFacade
import com.mcpservice.service.InventoryFacade
import com.mcpservice.service.MemberAccountFacade
import org.springframework.ai.mcp.annotation.McpResource
import org.springframework.core.io.ClassPathResource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

@Component
class LibraryResources(
    private val catalogFacade: CatalogFacade,
    private val inventoryFacade: InventoryFacade,
    private val memberAccountFacade: MemberAccountFacade,
    private val objectMapper: ObjectMapper
) {
    private val circulationGuide: String by lazy {
        ClassPathResource("mcp/circulation-guide.md")
            .getContentAsString(StandardCharsets.UTF_8)
    }

    @PreAuthorize("hasRole('library-user')")
    @McpResource(
        uri = "library://guide/circulation",
        name = "circulation-guide",
        title = "Library circulation guide",
        description = "Terminology and safety guidance for catalog, inventory, loan, fee, and payment workflows.",
        mimeType = "text/markdown"
    )
    fun circulationGuide(): String = circulationGuide

    @PreAuthorize("hasRole('library-user')")
    @McpResource(
        uri = "library://catalog/books/{bookId}",
        name = "catalog-book",
        title = "Catalog book",
        description = "A normalized catalog book record with explicit price currency.",
        mimeType = "application/json"
    )
    fun catalogBook(bookId: String): String = json(
        catalogFacade.getBook(McpArguments.identifier(bookId, "bookId"))
    )

    @PreAuthorize("hasRole('library-user')")
    @McpResource(
        uri = "library://inventory/books/{bookId}",
        name = "book-inventory",
        title = "Book inventory",
        description = "A bounded cross-library physical stock snapshot for one book.",
        mimeType = "application/json"
    )
    fun bookInventory(bookId: String): String = json(
        inventoryFacade.findCopies(McpArguments.identifier(bookId, "bookId"))
    )

    @PreAuthorize("hasRole('library-admin')")
    @McpResource(
        uri = "library://members/{memberId}/account",
        name = "member-account",
        title = "Member account",
        description = "Sensitive administrative account summary including subscription, loans, fees, payments, and bans.",
        mimeType = "application/json"
    )
    fun memberAccount(memberId: String): String = json(
        memberAccountFacade.getAccountSummary(McpArguments.identifier(memberId, "memberId"))
    )

    private fun json(value: Any): String = try {
        objectMapper.writeValueAsString(value)
    } catch (_: RuntimeException) {
        throw LibraryMcpToolException(ToolErrorCode.INTERNAL_ERROR, "The resource could not be serialized")
    }
}
