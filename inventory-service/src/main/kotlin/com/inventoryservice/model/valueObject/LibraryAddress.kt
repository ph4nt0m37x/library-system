package com.inventoryservice.model.valueObject

import com.inventoryservice.model.exception.DomainValidationException

@JvmInline
value class LibraryAddress private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 500

        fun of(rawValue: String): LibraryAddress {
            val value = rawValue.trim()
            if (value.isBlank()) {
                throw DomainValidationException("Library address must not be blank")
            }
            if (value.length > MAX_LENGTH) {
                throw DomainValidationException("Library address must not exceed $MAX_LENGTH characters")
            }
            return LibraryAddress(value)
        }
    }
}
