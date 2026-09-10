package com.borrowingservice.model.valueObject.dto

import com.borrowingservice.model.valueObject.enums.LoanStatus
import java.time.ZonedDateTime

data class CreateLoanDTO(
    val memberId: String,
    val bookId: String,
    val borrowedAt: ZonedDateTime
)

data class ExtendLoanDTO(val extendedAt: ZonedDateTime)

data class ReturnLoanDTO(val returnedAt: ZonedDateTime)

data class DeclareBookLostDTO(val declaredLostAt: ZonedDateTime)

data class RecordPermanentBookDamageDTO(val damageRecordedAt: ZonedDateTime)

data class LoanResponse(
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val borrowedAt: ZonedDateTime,
    val dueAt: ZonedDateTime,
    val extendedAt: ZonedDateTime?,
    val returnedAt: ZonedDateTime?,
    val status: LoanStatus,
    val incidentDeclaredAt: ZonedDateTime?
)
