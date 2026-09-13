package com.mcpservice.mcp

import com.mcpservice.projection.InventoryAvailabilityProjection
import com.mcpservice.service.InventoryFacade
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class InventoryTools(
    private val inventoryFacade: InventoryFacade
) {
    @PreAuthorize("hasRole('library-user')")
    @McpTool(
        name = "inventory_find_copies",
        title = "Find available book copies",
        description = "Find physical stock for a catalog book across library branches, or check one specific branch. Catalog availability alone does not prove that a copy is physically available.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Find available book copies",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun findCopies(
        @McpToolParam(description = "Catalog book identifier", required = true)
        bookId: String,
        @McpToolParam(description = "Optional library identifier; omit to search all branches", required = false)
        libraryId: String?
    ): InventoryAvailabilityProjection = inventoryFacade.findCopies(
        bookId = McpArguments.identifier(bookId, "bookId"),
        libraryId = libraryId?.let { McpArguments.identifier(it, "libraryId") }
    )
}
