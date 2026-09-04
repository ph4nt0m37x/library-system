package com.catalogservice.model.event

import com.catalogservice.model.valueObject.BookId


abstract class BookEvent(
    open val id: BookId
) : AbstractEvent(id)
