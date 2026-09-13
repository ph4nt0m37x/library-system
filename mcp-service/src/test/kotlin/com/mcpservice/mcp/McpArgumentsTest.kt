package com.mcpservice.mcp

import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class McpArgumentsTest {
    @Test
    fun `normalizes valid optional and bounded arguments`() {
        assertEquals("title", McpArguments.optionalSearch("  title  ", "title"))
        assertEquals(null, McpArguments.optionalSearch("   ", "title"))
        assertEquals(10, McpArguments.limit(null))
        assertEquals("USD", McpArguments.currency(" usd "))
        assertEquals(listOf("fee-1", "fee-2"), McpArguments.feeIds(listOf(" fee-1 ", "fee-2")))
    }

    @Test
    fun `rejects duplicate fees and unsafe bounds with stable code`() {
        val duplicate = assertFailsWith<LibraryMcpToolException> {
            McpArguments.feeIds(listOf("fee-1", "fee-1"))
        }
        assertEquals(ToolErrorCode.INVALID_ARGUMENT, duplicate.code)

        val badLimit = assertFailsWith<LibraryMcpToolException> { McpArguments.limit(51) }
        assertEquals(ToolErrorCode.INVALID_ARGUMENT, badLimit.code)
    }
}
