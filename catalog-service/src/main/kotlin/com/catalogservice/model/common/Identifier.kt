package com.catalogservice.model.common

import jakarta.persistence.MappedSuperclass
import java.io.Serializable

@MappedSuperclass
abstract class Identifier<T>(providedValue: String, @Transient val entityClass: Class<T>) : Serializable {

    val value = "${entityClass.simpleName}:${providedValue.replace(".*:".toRegex(), "")}"

    override fun hashCode(): Int {
        return this.entityClass.hashCode() + this.value.hashCode()
    }

    fun baseValue() = value.split(":")[1]

    fun prefixedValue() = value

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Identifier<*>

        if (entityClass != other.entityClass) return false
        if (value != other.value) return false

        return true
    }

    override fun toString(): String = value
}
