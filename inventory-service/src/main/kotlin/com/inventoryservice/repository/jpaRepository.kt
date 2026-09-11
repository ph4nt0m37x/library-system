package com.inventoryservice.repository

import com.inventoryservice.model.aggregate.Library
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.view.LibraryView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LibraryRepository : JpaRepository<Library, LibraryId>

@Repository
interface BookStockRepository : JpaRepository<BookStock, Long> {

    fun findByBookId(bookId: String): List<BookStock>

    fun findByLibraryId(libraryId: String): List<BookStock>

    fun findByLibraryIdAndBookId(
        libraryId: String,
        bookId: String
    ): BookStock?
}

@Repository
interface LibraryViewReadRepository : JpaRepository<LibraryView, LibraryId> {

}