package com.borrowingservice.service.impl

import com.borrowingservice.model.valueObject.dto.FeeResponse
import com.borrowingservice.model.valueObject.enums.FeeStatus
import com.borrowingservice.model.view.FeeView
import com.borrowingservice.repository.FeeViewRepository
import com.borrowingservice.service.FeeViewReadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FeeViewReadServiceImpl(
    private val feeViewRepository: FeeViewRepository
) : FeeViewReadService {
    override fun findById(feeId: String): FeeResponse? =
        feeViewRepository.findById(feeId).orElse(null)?.toResponse()

    override fun findAll(): List<FeeResponse> = feeViewRepository.findAll().map { it.toResponse() }

    override fun findByMemberId(memberId: String): List<FeeResponse> =
        feeViewRepository.findByMemberIdOrderByCreatedAtDesc(memberId).map { it.toResponse() }

    override fun findUnpaidByMemberId(memberId: String): List<FeeResponse> =
        feeViewRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(memberId, FeeStatus.UNPAID)
            .map { it.toResponse() }

    private fun FeeView.toResponse() = FeeResponse(
        feeId = feeId,
        loanId = loanId,
        memberId = memberId,
        currency = currency,
        reason = reason,
        status = status,
        createdAt = createdAt,
        dueAt = dueAt,
        returnedAt = returnedAt,
        incidentAt = incidentAt,
        settledAt = settledAt,
        settledByPaymentId = settledByPaymentId,
        settlementAllocationId = settlementAllocationId,
        settlementAmount = settlementAmount
    )
}
