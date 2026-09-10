package com.borrowingservice.model.event

import com.borrowingservice.model.valueObject.PaymentAllocationDetails
import java.math.BigDecimal
import java.time.ZonedDateTime

data class PaymentRecordedEvent(
    val paymentId: String,
    val memberId: String,
    val amount: BigDecimal,
    val currency: String,
    val paidAt: ZonedDateTime,
    val allocations: List<PaymentAllocationDetails>
)
