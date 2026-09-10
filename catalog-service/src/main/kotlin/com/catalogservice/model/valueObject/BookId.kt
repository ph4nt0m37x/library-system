package com.catalogservice.model.valueObject

import com.catalogservice.model.common.Identifier
import com.catalogservice.model.aggregate.Book
import jakarta.persistence.Embeddable
import java.util.UUID

@Embeddable
open class BookId(override val value: String) : Identifier<BookId>(value, BookId::class.java) {
    constructor() : this(UUID.randomUUID().toString())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        return this.value == (other as BookId).value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
