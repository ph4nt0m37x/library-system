package com.borrowingservice.service.impl

import com.borrowingservice.model.valueObject.dto.LoanResponse
import com.borrowingservice.model.valueObject.enums.LoanStatus
import com.borrowingservice.model.view.LoanView
import com.borrowingservice.repository.LoanViewRepository
import com.borrowingservice.service.LoanViewReadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class LoanViewReadServiceImpl(
    private val loanViewRepository: LoanViewRepository
) : LoanViewReadService {
    override fun findById(loanId: String): LoanResponse? =
        loanViewRepository.findById(loanId).orElse(null)?.toResponse()

    override fun findAll(): List<LoanResponse> = loanViewRepository.findAll().map { it.toResponse() }

    override fun findByMemberId(memberId: String): List<LoanResponse> =
        loanViewRepository.findByMemberIdOrderByBorrowedAtDesc(memberId).map { it.toResponse() }

    override fun findActiveByMemberId(memberId: String): List<LoanResponse> =
        loanViewRepository.findByMemberIdAndStatusOrderByBorrowedAtDesc(memberId, LoanStatus.ACTIVE)
            .map { it.toResponse() }

    private fun LoanView.toResponse() = LoanResponse(
        loanId = loanId,
        memberId = memberId,
        bookId = bookId,
        borrowedAt = borrowedAt,
        dueAt = dueAt,
        extendedAt = extendedAt,
        returnedAt = returnedAt,
        status = status,
        incidentDeclaredAt = incidentDeclaredAt,
        idempotencyKey = idempotencyKey
    )
}
