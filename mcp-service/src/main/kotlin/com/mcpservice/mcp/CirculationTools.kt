package com.mcpservice.mcp

import com.mcpservice.projection.FeeListProjection
import com.mcpservice.projection.LoanActionProjection
import com.mcpservice.projection.LoanProjection
import com.mcpservice.service.CirculationFacade
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class CirculationTools(
    private val circulationFacade: CirculationFacade
) {
    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "loan_get",
        title = "Get a loan",
        description = "Retrieve the authoritative current state and lifecycle timestamps of one loan.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Get a loan",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun getLoan(
        @McpToolParam(description = "Loan identifier", required = true)
        loanId: String
    ): LoanProjection = circulationFacade.getLoan(McpArguments.identifier(loanId, "loanId"))

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "fee_list_unpaid",
        title = "List a member's unpaid fees",
        description = "List the selected member's currently unpaid borrowing fees. Use payment_quote to obtain the exact payable amount.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "List a member's unpaid fees",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun listUnpaidFees(
        @McpToolParam(description = "Member identifier", required = true)
        memberId: String
    ): FeeListProjection =
        circulationFacade.listUnpaidFees(McpArguments.identifier(memberId, "memberId"))

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "loan_create",
        title = "Create a library loan",
        description = "Create a loan after the user has selected an exact member, book, and lending branch and confirmed the action. Borrowing performs all authoritative eligibility checks. Reuse the same idempotency key only for an identical retry.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Create a library loan",
            readOnlyHint = false,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun createLoan(
        @McpToolParam(description = "Member identifier", required = true)
        memberId: String,
        @McpToolParam(description = "Catalog book identifier", required = true)
        bookId: String,
        @McpToolParam(description = "Lending library identifier", required = true)
        libraryId: String,
        @McpToolParam(description = "Stable 1-100 character key reused only for an identical retry", required = true)
        idempotencyKey: String
    ): LoanActionProjection = circulationFacade.createLoan(
        memberId = McpArguments.identifier(memberId, "memberId"),
        bookId = McpArguments.identifier(bookId, "bookId"),
        libraryId = McpArguments.identifier(libraryId, "libraryId"),
        idempotencyKey = McpArguments.idempotencyKey(idempotencyKey)
    )

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "loan_extend",
        title = "Extend a loan",
        description = "Extend an eligible active loan once. Call only after the user confirms the exact loan. The server clock determines the extension time.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Extend a loan",
            readOnlyHint = false,
            destructiveHint = false,
            idempotentHint = false,
            openWorldHint = false
        )
    )
    fun extendLoan(
        @McpToolParam(description = "Active loan identifier", required = true)
        loanId: String
    ): LoanActionProjection = circulationFacade.extendLoan(McpArguments.identifier(loanId, "loanId"))

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "loan_return",
        title = "Return a loan",
        description = "Irreversibly mark an active loan returned. Call only after explicit user confirmation. The server clock is authoritative and inventory updates asynchronously.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Return a loan",
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false,
            openWorldHint = false
        )
    )
    fun returnLoan(
        @McpToolParam(description = "Active loan identifier", required = true)
        loanId: String
    ): LoanActionProjection = circulationFacade.returnLoan(McpArguments.identifier(loanId, "loanId"))

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "loan_report_lost",
        title = "Report a loaned book lost",
        description = "Irreversibly mark a loaned book lost. Call only after explicit user confirmation. This may create a replacement-price fee and reduce total inventory.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Report a loaned book lost",
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false,
            openWorldHint = false
        )
    )
    fun reportLost(
        @McpToolParam(description = "Active loan identifier", required = true)
        loanId: String
    ): LoanActionProjection = circulationFacade.reportLost(McpArguments.identifier(loanId, "loanId"))

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "loan_report_damage",
        title = "Report permanent book damage",
        description = "Irreversibly mark a loaned book permanently damaged. Call only after explicit user confirmation. This may create a fee, reduce stock, and contribute to a borrowing ban.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Report permanent book damage",
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = false,
            openWorldHint = false
        )
    )
    fun reportDamage(
        @McpToolParam(description = "Active loan identifier", required = true)
        loanId: String
    ): LoanActionProjection = circulationFacade.reportDamage(McpArguments.identifier(loanId, "loanId"))
}
