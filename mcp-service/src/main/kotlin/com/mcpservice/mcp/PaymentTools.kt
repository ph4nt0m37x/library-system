package com.mcpservice.mcp

import com.mcpservice.projection.PaymentQuoteProjection
import com.mcpservice.projection.PaymentRecordProjection
import com.mcpservice.service.PaymentFacade
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class PaymentTools(
    private val paymentFacade: PaymentFacade
) {
    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "payment_quote",
        title = "Quote a fee payment",
        description = "Calculate the exact amount for a nonempty selected set of a member's unpaid fees. This read-only operation creates no payment and charges no payment instrument.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Quote a fee payment",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = false,
            openWorldHint = false
        )
    )
    fun quote(
        @McpToolParam(description = "Member identifier", required = true)
        memberId: String,
        @McpToolParam(description = "One to 50 unique unpaid fee identifiers", required = true)
        feeIds: List<String>,
        @McpToolParam(description = "Three-letter currency code", required = true)
        currency: String
    ): PaymentQuoteProjection = paymentFacade.quote(
        memberId = McpArguments.identifier(memberId, "memberId"),
        feeIds = McpArguments.feeIds(feeIds),
        currency = McpArguments.currency(currency)
    )

    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "payment_record",
        title = "Record a fee payment",
        description = "Record and allocate a previously quoted fee payment after explicit user confirmation. This changes financial records but does not charge an external card/account. Use the exact unchanged payment ID, amount, currency, member, and fee IDs from payment_quote.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Record a fee payment",
            readOnlyHint = false,
            destructiveHint = true,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun record(
        @McpToolParam(description = "Payment identifier returned by payment_quote", required = true)
        paymentId: String,
        @McpToolParam(description = "Member identifier from the quote", required = true)
        memberId: String,
        @McpToolParam(description = "Exact positive decimal amount from the quote", required = true)
        amount: BigDecimal,
        @McpToolParam(description = "Three-letter currency code from the quote", required = true)
        currency: String,
        @McpToolParam(description = "Exact unique fee identifier set from the quote", required = true)
        feeIds: List<String>
    ): PaymentRecordProjection = paymentFacade.record(
        paymentId = McpArguments.identifier(paymentId, "paymentId"),
        memberId = McpArguments.identifier(memberId, "memberId"),
        amount = McpArguments.amount(amount),
        currency = McpArguments.currency(currency),
        feeIds = McpArguments.feeIds(feeIds)
    )
}
