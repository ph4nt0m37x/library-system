package com.catalogservice.repository

import com.catalogservice.model.aggregate.Book
import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.view.BookView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BookRepository : JpaRepository<Book, BookId>

@Repository
interface BookCategoryRepository : JpaRepository<BookCategory, Long> {

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: Long): Boolean
}

interface BookViewRepository : JpaRepository<BookView, BookId> {

    fun findAllByDeletedFalse(): List<BookView>

    fun existsByIdAndDeletedFalse(id: BookId): Boolean

    fun existsByIsbn(isbn: String): Boolean

    fun existsByIsbnAndIdNot(isbn: String, id: BookId): Boolean

    fun existsByCategory_Id(categoryId: Long): Boolean

    fun findByTitleContainingIgnoreCase(title: String): List<BookView>

    fun findByAuthorContainingIgnoreCase(author: String): List<BookView>

    fun findByCategory_Id(categoryId: Long): List<BookView>
}

