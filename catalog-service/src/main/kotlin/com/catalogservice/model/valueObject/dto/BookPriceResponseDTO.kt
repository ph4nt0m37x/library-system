package com.catalogservice.model.valueObject.dto

import java.math.BigDecimal

data class BookPriceResponseDTO(
    val amount: BigDecimal,
    val currency: String
)
