package com.borrowingservice.service

import com.borrowingservice.model.valueObject.dto.PaymentResponse

interface PaymentViewReadService {
    fun findById(paymentId: String): PaymentResponse?
    fun findAll(): List<PaymentResponse>
    fun findByMemberId(memberId: String): List<PaymentResponse>
}
