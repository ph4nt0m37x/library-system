package com.borrowingservice.service

import com.borrowingservice.model.valueObject.dto.FeeResponse

interface FeeViewReadService {
    fun findById(feeId: String): FeeResponse?
    fun findAll(): List<FeeResponse>
    fun findByMemberId(memberId: String): List<FeeResponse>
    fun findUnpaidByMemberId(memberId: String): List<FeeResponse>
}
