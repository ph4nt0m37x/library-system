package com.borrowingservice.service

import com.borrowingservice.model.valueObject.dto.LoanResponse

interface LoanViewReadService {
    fun findById(loanId: String): LoanResponse?
    fun findAll(): List<LoanResponse>
    fun findByMemberId(memberId: String): List<LoanResponse>
    fun findActiveByMemberId(memberId: String): List<LoanResponse>
}
