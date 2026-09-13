package com.mcpservice.projection

data class LoanProjection(
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val libraryId: String?,
    val borrowedAt: String,
    val dueAt: String,
    val extendedAt: String?,
    val returnedAt: String?,
    val status: String,
    val incidentDeclaredAt: String?
)

data class LoanActionProjection(
    val loanId: String,
    val action: String,
    val inventoryUpdatePending: Boolean,
    val message: String
)
