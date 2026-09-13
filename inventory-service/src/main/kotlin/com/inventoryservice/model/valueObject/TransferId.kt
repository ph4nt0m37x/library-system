package com.inventoryservice.model.valueObject


import com.inventoryservice.model.common.Identifier
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
open class TransferId(override val value: String) : Identifier<TransferId>(value, TransferId::class.java) {
    constructor() : this(UUID.randomUUID().toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return this.value == (other as TransferId).value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
