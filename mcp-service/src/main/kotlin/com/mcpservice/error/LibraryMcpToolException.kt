package com.mcpservice.error

enum class ToolErrorCode {
    INVALID_ARGUMENT,
    UNAUTHENTICATED,
    FORBIDDEN,
    BOOK_NOT_FOUND,
    LIBRARY_NOT_FOUND,
    MEMBER_NOT_FOUND,
    LOAN_NOT_FOUND,
    FEE_NOT_FOUND,
    MEMBERSHIP_INACTIVE,
    NO_STOCK,
    LOAN_CONFLICT,
    PAYMENT_CONFLICT,
    DEPENDENCY_UNAVAILABLE,
    DOWNSTREAM_CONTRACT_ERROR,
    INTERNAL_ERROR
}

class LibraryMcpToolException(
    val code: ToolErrorCode,
    safeMessage: String
) : RuntimeException("${code.name}: $safeMessage") {
    constructor(code: String, safeMessage: String) : this(
        parseCode(code),
        safeMessage
    )

    companion object {
        private fun parseCode(code: String): ToolErrorCode =
            ToolErrorCode.entries.firstOrNull { it.name == code } ?: ToolErrorCode.INTERNAL_ERROR
    }
}
