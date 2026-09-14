package com.inventoryservice.model.valueObject


import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import com.inventoryservice.model.common.Identifier
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
open class TransferId(
    @get:JsonValue
    override val value: String
) : Identifier<TransferId>(value, TransferId::class.java) {
    constructor() : this(UUID.randomUUID().toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return this.value == (other as TransferId).value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromJson(value: String): TransferId = TransferId(value)
    }
}
