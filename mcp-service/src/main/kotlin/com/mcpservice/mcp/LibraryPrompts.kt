package com.mcpservice.mcp

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult
import io.modelcontextprotocol.spec.McpSchema.PromptMessage
import io.modelcontextprotocol.spec.McpSchema.Role
import io.modelcontextprotocol.spec.McpSchema.TextContent
import org.springframework.ai.mcp.annotation.McpArg
import org.springframework.ai.mcp.annotation.McpPrompt
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class LibraryPrompts {
    @PreAuthorize("hasRole('library-user')")
    @McpPrompt(
        name = "find_available_book",
        title = "Find an available book",
        description = "Guide a catalog search followed by a real physical-inventory check."
    )
    fun findAvailableBook(
        @McpArg(name = "title", description = "Optional title fragment", required = false)
        title: String?,
        @McpArg(name = "author", description = "Optional author fragment", required = false)
        author: String?,
        @McpArg(name = "categoryId", description = "Optional category identifier", required = false)
        categoryId: String?,
        @McpArg(name = "preferredLibrary", description = "Optional preferred library name or identifier", required = false)
        preferredLibrary: String?
    ): GetPromptResult {
        val filters = listOfNotNull(
            title?.takeIf { it.isNotBlank() }?.let { "title '$it'" },
            author?.takeIf { it.isNotBlank() }?.let { "author '$it'" },
            categoryId?.takeIf { it.isNotBlank() }?.let { "category '$it'" }
        ).joinToString().ifBlank { "no initial filter" }
        val preference = preferredLibrary?.takeIf { it.isNotBlank() }
            ?.let { " Prefer library '$it' when it has an available copy." }
            ?: ""

        return prompt(
            "Find a library book using catalog_search with $filters. If several books match, ask the user to choose one. " +
                "For the selected book, call inventory_find_copies before claiming it can be borrowed.$preference " +
                "Report identifiers and actual available quantities. Do not treat catalog presence as physical availability."
        )
    }

    @PreAuthorize("hasRole('library-admin')")
    @McpPrompt(
        name = "review_member_account",
        title = "Review a member account",
        description = "Guide an administrative review of membership, active loans, unpaid fees, payments, and bans."
    )
    fun reviewMemberAccount(
        @McpArg(name = "memberId", description = "Member identifier", required = true)
        memberId: String
    ): GetPromptResult = prompt(
        "Call member_get_account_summary for member '${McpArguments.identifier(memberId, "memberId")}'. " +
            "Summarize subscription eligibility and end date, active loans and due dates, unpaid fees, recent payments, " +
            "and current borrowing-ban status. Minimize repetition of email and phone data unless explicitly needed. " +
            "Do not invent missing records or present partial data as complete."
    )

    @PreAuthorize("hasRole('library-admin')")
    @McpPrompt(
        name = "borrow_book",
        title = "Borrow a book safely",
        description = "Guide a safe search, availability, account review, confirmation, and idempotent loan workflow."
    )
    fun borrowBook(
        @McpArg(name = "memberId", description = "Member identifier", required = true)
        memberId: String,
        @McpArg(name = "book", description = "Optional book title or identifier", required = false)
        book: String?,
        @McpArg(name = "preferredLibrary", description = "Optional preferred library name or identifier", required = false)
        preferredLibrary: String?
    ): GetPromptResult {
        val target = book?.takeIf { it.isNotBlank() } ?: "the book requested by the user"
        val preferred = preferredLibrary?.takeIf { it.isNotBlank() } ?: "any suitable branch"
        return prompt(
            "Help member '${McpArguments.identifier(memberId, "memberId")}' borrow $target from $preferred. " +
                "Resolve an exact book with catalog_search/catalog_get_book, verify real stock with inventory_find_copies, " +
                "and review eligibility with member_get_account_summary. Show the exact member, book, and library and ask " +
                "for explicit confirmation before loan_create. Generate one stable idempotency key for the confirmed attempt " +
                "and reuse it only for an identical retry. After success, explain that inventory changes asynchronously."
        )
    }

    private fun prompt(message: String): GetPromptResult =
        GetPromptResult.builder(
            listOf(PromptMessage(Role.USER, TextContent.builder(message).build()))
        ).description("Library System guided workflow").build()
}
