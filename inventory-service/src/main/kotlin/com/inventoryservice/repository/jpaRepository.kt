package com.inventoryservice.repository

import com.inventoryservice.model.aggregate.Library
import com.inventoryservice.model.aggregate.Transfer
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.model.view.LibraryView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LibraryRepository : JpaRepository<Library, LibraryId> {

    fun existsByNameAndAddress(name: String, address: String): Boolean

    fun existsByNameAndAddressAndIdNot(
        name: String,
        address: String,
        id: LibraryId
    ): Boolean
}

@Repository
interface TransferRepository : JpaRepository<Transfer, TransferId>

@Repository
interface BookStockRepository : JpaRepository<BookStock, Long> {

    fun findByBookId(bookId: BookId): List<BookStock>

    fun findByLibraryId(libraryId: String): List<BookStock>

    fun findByLibraryIdAndBookId(
        libraryId: String,
        bookId: BookId
    ): BookStock?
}

@Repository
interface LibraryViewReadRepository : JpaRepository<LibraryView, LibraryId> {

    fun findAllByDeletedFalse(): List<LibraryView>

}
