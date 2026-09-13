package com.mcpservice.projection

import tools.jackson.databind.JsonNode
import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import java.util.UUID

object IdentifierNormalizer {
    fun fromWire(node: JsonNode?): String {
        if (node == null || node.isNull) {
            throw LibraryMcpToolException(
                ToolErrorCode.DOWNSTREAM_CONTRACT_ERROR,
                "A downstream service returned an identifier in an unsupported form."
            )
        }
        val raw = when {
            node.isTextual -> node.asText()
            node.isObject && node.hasNonNull("value") -> node.get("value").asText()
            node.isObject && node.hasNonNull("id") -> return fromWire(node.get("id"))
            else -> throw LibraryMcpToolException(
                ToolErrorCode.DOWNSTREAM_CONTRACT_ERROR,
                "A downstream service returned an identifier in an unsupported form."
            )
        }
        return base(raw)
    }

    fun base(raw: String): String = raw.trim().substringAfterLast(':').trim()

    fun requireUuid(raw: String, argumentName: String): String {
        val value = base(raw)
        if (value.isBlank()) {
            throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, "$argumentName must not be blank.")
        }
        val parsed = try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, "$argumentName must be a valid UUID.")
        }
        if (!parsed.toString().equals(value, ignoreCase = true)) {
            throw LibraryMcpToolException(ToolErrorCode.INVALID_ARGUMENT, "$argumentName must be a valid UUID.")
        }
        return parsed.toString()
    }
}
