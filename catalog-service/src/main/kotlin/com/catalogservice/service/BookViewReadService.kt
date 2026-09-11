package com.catalogservice.service

import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.valueObject.dto.BookPriceResponseDTO
import com.catalogservice.model.view.BookView

interface BookViewReadService {
    fun findById(id: BookId): BookView?
    fun findAll(): List<BookView>
    fun findAllAvailable(): List<BookView>
    fun existsByIsbn(isbn: String): Boolean
    fun existsByIsbnAndIdNot(isbn: String, id: BookId): Boolean
    fun searchByTitle(title: String): List<BookView>
    fun searchByAuthor(author: String): List<BookView>
    fun filterByCategory(categoryId: Long): List<BookView>
    fun getBookPrice(bookId: String): BookPriceResponseDTO?
}

