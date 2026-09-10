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
interface BookCategoryRepository : JpaRepository<BookCategory, Long>

interface BookViewRepository : JpaRepository<BookView, BookId> {

    fun findByTitleContainingIgnoreCase(title: String): List<BookView>

    fun findByAuthorContainingIgnoreCase(author: String): List<BookView>

    fun findByCategory_Id(categoryId: Long): List<BookView>
}

