package com.mcpservice.mcp

import com.mcpservice.projection.MemberAccountProjection
import com.mcpservice.service.MemberAccountFacade
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component

@Component
class MemberTools(
    private val memberAccountFacade: MemberAccountFacade
) {
    @PreAuthorize("hasRole('library-admin')")
    @McpTool(
        name = "member_get_account_summary",
        title = "Review a member account",
        description = "Administrative tool that combines member and subscription details with active loans, unpaid fees, payment history, and current borrowing-ban state. Contains sensitive member data.",
        generateOutputSchema = true,
        annotations = McpTool.McpAnnotations(
            title = "Review a member account",
            readOnlyHint = true,
            destructiveHint = false,
            idempotentHint = true,
            openWorldHint = false
        )
    )
    fun getAccountSummary(
        @McpToolParam(description = "Member identifier", required = true)
        memberId: String
    ): MemberAccountProjection =
        memberAccountFacade.getAccountSummary(McpArguments.identifier(memberId, "memberId"))
}
