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

@Repository
interface BookViewRepository : JpaRepository<BookView, BookId>

