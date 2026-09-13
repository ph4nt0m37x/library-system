package com.mcpservice.mcp

import org.springframework.ai.mcp.annotation.McpPrompt
import org.springframework.ai.mcp.annotation.McpResource
import org.springframework.ai.mcp.annotation.McpTool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class McpCapabilityMetadataTest {
    private val toolClasses = listOf(
        CatalogTools::class.java,
        InventoryTools::class.java,
        MemberTools::class.java,
        CirculationTools::class.java,
        PaymentTools::class.java
    )

    @Test
    fun `publishes the intended stable tool catalog`() {
        val tools = toolClasses.flatMap { type ->
            type.declaredMethods.mapNotNull { it.getAnnotation(McpTool::class.java) }
        }
        assertEquals(
            setOf(
                "catalog_search",
                "catalog_get_book",
                "inventory_find_copies",
                "member_get_account_summary",
                "loan_get",
                "fee_list_unpaid",
                "loan_create",
                "loan_extend",
                "loan_return",
                "loan_report_lost",
                "loan_report_damage",
                "payment_quote",
                "payment_record"
            ),
            tools.map { it.name }.toSet()
        )
        assertEquals(13, tools.size)
        assertTrue(tools.all { it.generateOutputSchema })
        assertTrue(tools.all { !it.annotations.openWorldHint })

        val create = tools.single { it.name == "loan_create" }
        assertTrue(create.annotations.idempotentHint)
        assertFalse(create.annotations.destructiveHint)

        val destructive = tools.filter { it.annotations.destructiveHint }.map { it.name }.toSet()
        assertEquals(
            setOf("loan_return", "loan_report_lost", "loan_report_damage", "payment_record"),
            destructive
        )
    }

    @Test
    fun `publishes four resources and three prompts`() {
        val resources = LibraryResources::class.java.declaredMethods
            .mapNotNull { it.getAnnotation(McpResource::class.java) }
        val prompts = LibraryPrompts::class.java.declaredMethods
            .mapNotNull { it.getAnnotation(McpPrompt::class.java) }

        assertEquals(4, resources.size)
        assertEquals(
            setOf(
                "library://guide/circulation",
                "library://catalog/books/{bookId}",
                "library://inventory/books/{bookId}",
                "library://members/{memberId}/account"
            ),
            resources.map { it.uri }.toSet()
        )
        assertEquals(
            setOf("find_available_book", "review_member_account", "borrow_book"),
            prompts.map { it.name }.toSet()
        )
    }
}
