package com.borrowingservice.model.valueObject.dto

import com.borrowingservice.model.valueObject.enums.LoanStatus
import java.time.ZonedDateTime

data class CreateLoanDTO(
    val memberId: String,
    val bookId: String,
    val libraryId: String,
    @Deprecated("Ignored; normal loan creation uses the server Clock")
    val borrowedAt: ZonedDateTime? = null,
    val idempotencyKey: String? = null
)

@Deprecated("Ignored; normal loan extension uses the server Clock")
data class ExtendLoanDTO(val extendedAt: ZonedDateTime? = null)

@Deprecated("Ignored; normal loan return uses the server Clock")
data class ReturnLoanDTO(val returnedAt: ZonedDateTime? = null)

@Deprecated("Ignored; normal lost-book declaration uses the server Clock")
data class DeclareBookLostDTO(val declaredLostAt: ZonedDateTime? = null)

@Deprecated("Ignored; normal damage recording uses the server Clock")
data class RecordPermanentBookDamageDTO(val damageRecordedAt: ZonedDateTime? = null)

data class LoanResponse(
    val loanId: String,
    val memberId: String,
    val bookId: String,
    val libraryId: String?,
    val borrowedAt: ZonedDateTime,
    val dueAt: ZonedDateTime,
    val extendedAt: ZonedDateTime?,
    val returnedAt: ZonedDateTime?,
    val status: LoanStatus,
    val incidentDeclaredAt: ZonedDateTime?,
    val idempotencyKey: String? = null
)
