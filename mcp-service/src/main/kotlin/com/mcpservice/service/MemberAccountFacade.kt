package com.mcpservice.service

import com.mcpservice.client.BorrowingClient
import com.mcpservice.client.MembershipClient
import com.mcpservice.error.ToolErrorCode
import com.mcpservice.projection.IdentifierNormalizer
import com.mcpservice.projection.MemberAccountProjection
import org.springframework.stereotype.Service

@Service
class MemberAccountFacade(
    private val membershipClient: MembershipClient,
    private val borrowingClient: BorrowingClient,
    private val errors: DownstreamErrorTranslator
) {
    fun getAccountSummary(memberId: String): MemberAccountProjection {
        val id = IdentifierNormalizer.requireUuid(memberId, "memberId")
        val member = errors.call("Membership service", ToolErrorCode.MEMBER_NOT_FOUND) {
            membershipClient.findById(id)
        }
        val loans = errors.call("Borrowing service", ToolErrorCode.MEMBER_NOT_FOUND) {
            borrowingClient.findActiveLoans(id)
        }
        val fees = errors.call("Borrowing service", ToolErrorCode.MEMBER_NOT_FOUND) {
            borrowingClient.findUnpaidFees(id)
        }
        val payments = errors.call("Borrowing service", ToolErrorCode.MEMBER_NOT_FOUND) {
            borrowingClient.findPayments(id)
        }.sortedByDescending { it.paidAt }
        val ban = errors.optionalOnNotFound("Borrowing service") {
            borrowingClient.findBan(id)
        }
        return MemberAccountProjection(
            member = member.toProjection(),
            activeLoans = loans.map { it.toProjection() },
            unpaidFees = fees.map { it.toProjection() },
            recentPayments = payments.take(PAYMENT_LIMIT).map { it.toProjection() },
            currentBan = ban?.toProjection(),
            paymentLimit = PAYMENT_LIMIT,
            paymentsTruncated = payments.size > PAYMENT_LIMIT
        )
    }

    companion object {
        const val PAYMENT_LIMIT = 50
    }
}
