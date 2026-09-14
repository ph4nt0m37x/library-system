package com.inventoryservice.model.common

import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.TransferId
import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifierTests {
    @Test
    fun `base value accepts both prefixed and unprefixed IDs`() {
        assertEquals("book-1", BookId("book-1").baseValue())
        assertEquals("book-1", BookId("BookId:book-1").baseValue())
        assertEquals("transfer-1", TransferId("transfer-1").baseValue())
    }
}
