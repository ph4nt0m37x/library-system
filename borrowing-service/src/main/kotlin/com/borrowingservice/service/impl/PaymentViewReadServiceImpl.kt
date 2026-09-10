package com.borrowingservice.service.impl

import com.borrowingservice.model.valueObject.dto.PaymentAllocationResponse
import com.borrowingservice.model.valueObject.dto.PaymentResponse
import com.borrowingservice.model.view.PaymentView
import com.borrowingservice.repository.PaymentViewRepository
import com.borrowingservice.service.PaymentViewReadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PaymentViewReadServiceImpl(
    private val paymentViewRepository: PaymentViewRepository
) : PaymentViewReadService {
    override fun findById(paymentId: String): PaymentResponse? =
        paymentViewRepository.findById(paymentId).orElse(null)?.toResponse()

    override fun findAll(): List<PaymentResponse> = paymentViewRepository.findAll().map { it.toResponse() }

    override fun findByMemberId(memberId: String): List<PaymentResponse> =
        paymentViewRepository.findByMemberIdOrderByPaidAtDesc(memberId).map { it.toResponse() }

    private fun PaymentView.toResponse() = PaymentResponse(
        paymentId = paymentId,
        memberId = memberId,
        amount = amount,
        currency = currency,
        paidAt = paidAt,
        allocations = allocations.map {
            PaymentAllocationResponse(
                allocationId = it.allocationId,
                feeId = it.feeId,
                amount = it.amount
            )
        }
    )
}
