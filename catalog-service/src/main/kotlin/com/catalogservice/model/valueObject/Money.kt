package com.catalogservice.model.valueObject


import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.math.BigDecimal

@Embeddable
data class Money(

    @Column(name = "amount")
    val amount: BigDecimal

) : Serializable {

    init {
        require(amount > BigDecimal.ZERO) {
            "Price must be greater than zero"
        }
    }
}