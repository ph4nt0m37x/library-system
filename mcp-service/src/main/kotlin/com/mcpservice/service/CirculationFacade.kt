package com.mcpservice.service

import com.mcpservice.client.BorrowingClient
import com.mcpservice.client.dto.CreateLoanWire
import com.mcpservice.error.LibraryMcpToolException
import com.mcpservice.error.ToolErrorCode
import com.mcpservice.projection.FeeListProjection
import com.mcpservice.projection.IdentifierNormalizer
import com.mcpservice.projection.LoanActionProjection
import com.mcpservice.projection.LoanProjection
import org.springframework.stereotype.Service

@Service
class CirculationFacade(
    private val borrowingClient: BorrowingClient,
    private val errors: DownstreamErrorTranslator
) {
    fun getLoan(loanId: String): LoanProjection {
        val id = IdentifierNormalizer.requireUuid(loanId, "loanId")
        return errors.call("Borrowing service", ToolErrorCode.LOAN_NOT_FOUND) {
            borrowingClient.findLoan(id)
        }.toProjection()
    }

    fun listUnpaidFees(memberId: String): FeeListProjection {
        val id = IdentifierNormalizer.requireUuid(memberId, "memberId")
        val fees = errors.call("Borrowing service", ToolErrorCode.MEMBER_NOT_FOUND) {
            borrowingClient.findUnpaidFees(id)
        }
        val selected = fees.take(FEE_LIMIT).map { it.toProjection() }
        return FeeListProjection(selected, selected.size, FEE_LIMIT, fees.size > FEE_LIMIT)
    }

    fun createLoan(memberId: String, bookId: String, libraryId: String, idempotencyKey: String): LoanActionProjection {
        val key = idempotencyKey.trim()
        if (key.length !in 1..100) {
            throw LibraryMcpToolException(
                ToolErrorCode.INVALID_ARGUMENT,
                "idempotencyKey must contain between 1 and 100 characters."
            )
        }
        val response = errors.call("Borrowing service", ToolErrorCode.LOAN_NOT_FOUND) {
            borrowingClient.createLoan(
                CreateLoanWire(
                    memberId = IdentifierNormalizer.requireUuid(memberId, "memberId"),
                    bookId = IdentifierNormalizer.requireUuid(bookId, "bookId"),
                    libraryId = IdentifierNormalizer.requireUuid(libraryId, "libraryId"),
                    idempotencyKey = key
                )
            )
        }
        return action(response.id, "CREATED", true, "Loan created; inventory availability may update asynchronously.")
    }

    fun extendLoan(loanId: String): LoanActionProjection = mutate(
        loanId, "EXTENDED", false, "Loan extended."
    ) { borrowingClient.extendLoan(it).id }

    fun returnLoan(loanId: String): LoanActionProjection = mutate(
        loanId, "RETURNED", true, "Loan returned; inventory availability may update asynchronously."
    ) { borrowingClient.returnLoan(it).id }

    fun reportLost(loanId: String): LoanActionProjection = mutate(
        loanId, "REPORTED_LOST", true, "Book reported lost; fees and inventory may update asynchronously."
    ) { borrowingClient.reportLost(it).id }

    fun reportDamage(loanId: String): LoanActionProjection = mutate(
        loanId, "REPORTED_DAMAGED", true, "Book damage recorded; fees, bans, and inventory may update asynchronously."
    ) { borrowingClient.reportDamage(it).id }

    private fun mutate(
        loanId: String,
        action: String,
        inventoryPending: Boolean,
        message: String,
        command: (String) -> String
    ): LoanActionProjection {
        val id = IdentifierNormalizer.requireUuid(loanId, "loanId")
        val returnedId = errors.call("Borrowing service", ToolErrorCode.LOAN_NOT_FOUND) { command(id) }
        return action(returnedId, action, inventoryPending, message)
    }

    private fun action(id: String, action: String, pending: Boolean, message: String) = LoanActionProjection(
        loanId = IdentifierNormalizer.base(id),
        action = action,
        inventoryUpdatePending = pending,
        message = message
    )

    companion object {
        const val FEE_LIMIT = 50
    }
}
