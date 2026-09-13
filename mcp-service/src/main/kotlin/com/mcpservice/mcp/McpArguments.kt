package com.mcpservice.mcp

import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import java.math.BigDecimal

internal object McpArguments {
    private const val MAX_IDENTIFIER_LENGTH = 100
    private const val MAX_SEARCH_LENGTH = 200
    private const val MAX_FEE_IDS = 50

    fun identifier(value: String, label: String): String =
        value.trim().also {
            if (it.isBlank() || it.length > MAX_IDENTIFIER_LENGTH) {
                invalid("$label must contain between 1 and $MAX_IDENTIFIER_LENGTH characters")
            }
        }

    fun idempotencyKey(value: String): String =
        identifier(value, "idempotencyKey")

    fun optionalSearch(value: String?, label: String): String? =
        value?.trim()?.takeIf { it.isNotEmpty() }?.also {
            if (it.length > MAX_SEARCH_LENGTH) {
                invalid("$label must not exceed $MAX_SEARCH_LENGTH characters")
            }
        }

    fun categoryId(value: Long?): Long? =
        value?.also {
            if (it <= 0) invalid("categoryId must be greater than zero")
        }

    fun limit(value: Int?): Int = (value ?: 10).also {
        if (it !in 1..50) invalid("limit must be between 1 and 50")
    }

    fun currency(value: String): String =
        value.trim().uppercase().also {
            if (!it.matches(Regex("^[A-Z]{3}$"))) {
                invalid("currency must be a three-letter ISO currency code")
            }
        }

    fun feeIds(values: List<String>): List<String> {
        if (values.isEmpty()) invalid("feeIds must contain at least one fee ID")
        if (values.size > MAX_FEE_IDS) invalid("feeIds must not contain more than $MAX_FEE_IDS entries")

        val normalized = values.map { identifier(it, "feeId") }
        if (normalized.toSet().size != normalized.size) {
            invalid("feeIds must not contain duplicates")
        }
        return normalized
    }

    fun amount(value: BigDecimal): BigDecimal = value.also {
        if (it <= BigDecimal.ZERO) invalid("amount must be greater than zero")
        if (it.scale() > 2) invalid("amount must have at most two decimal places")
    }

    private fun invalid(message: String): Nothing =
        throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, message)
}
