package com.borrowingservice.service

import com.borrowingservice.model.valueObject.dto.BorrowingBanResponse

interface BorrowingBanViewReadService {
    fun findById(banRecordId: String): BorrowingBanResponse?
    fun findAll(): List<BorrowingBanResponse>
    fun findByMemberId(memberId: String): BorrowingBanResponse?
}
