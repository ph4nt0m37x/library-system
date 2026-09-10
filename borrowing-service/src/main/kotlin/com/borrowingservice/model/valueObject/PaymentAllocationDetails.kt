package com.borrowingservice.model.valueObject

import java.math.BigDecimal

data class PaymentAllocationDetails(
    val allocationId: String,
    val feeId: String,
    val amount: BigDecimal
)
