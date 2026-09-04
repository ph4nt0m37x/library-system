package com.catalogservice.service

import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.view.BookView

interface BookViewReadService {
    fun findById(id: BookId): BookView?
    fun findAll(): List<BookView>
}

