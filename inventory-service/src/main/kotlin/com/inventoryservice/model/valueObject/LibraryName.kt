package com.inventoryservice.model.valueObject

import com.inventoryservice.model.exception.DomainValidationException

@JvmInline
value class LibraryName private constructor(val value: String) {
    companion object {
        const val MAX_LENGTH = 200

        fun of(rawValue: String): LibraryName {
            val value = rawValue.trim()
            if (value.isBlank()) {
                throw DomainValidationException("Library name must not be blank")
            }
            if (value.length > MAX_LENGTH) {
                throw DomainValidationException("Library name must not exceed $MAX_LENGTH characters")
            }
            return LibraryName(value)
        }
    }
}
