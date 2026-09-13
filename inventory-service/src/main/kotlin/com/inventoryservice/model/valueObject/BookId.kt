package com.inventoryservice.model.valueObject

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
open class BookId @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(rawValue: String) : Serializable {

    @get:JsonValue
    @Column(name = "value", nullable = false)
    val value: String = normalize(rawValue)

    fun baseValue(): String = value.removePrefix("BookId:")

    override fun equals(other: Any?): Boolean =
        this === other || other is BookId && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        private fun normalize(value: String): String {
            val rawId = value.trim().removePrefix("BookId:")
            require(rawId.isNotBlank()) { "Book ID must not be blank" }

            val uuid = try {
                UUID.fromString(rawId)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Book ID must be a valid UUID")
            }

            require(uuid.toString().equals(rawId, ignoreCase = true)) {
                "Book ID must be a valid UUID"
            }

            return "BookId:$uuid"
        }
    }
}
