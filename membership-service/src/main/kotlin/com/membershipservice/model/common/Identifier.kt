package com.membershipservice.model.common

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import java.io.Serializable

@MappedSuperclass
abstract class Identifier<T>(
    providedValue: String,
    @field:Transient val entityClass: Class<T>
) : Serializable {

    val value = "${entityClass.simpleName}:${providedValue.replace(".*:".toRegex(), "")}"

    fun baseValue(): String = value.substringAfter(":")

    fun prefixedValue(): String = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Identifier<*>

        return entityClass == other.entityClass && value == other.value
    }

    override fun hashCode(): Int = entityClass.hashCode() + value.hashCode()

    override fun toString(): String = value
}
