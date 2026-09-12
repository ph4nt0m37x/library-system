package com.inventoryservice.service.impl

import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.repository.BookStockRepository
import org.springframework.stereotype.Service
import com.inventoryservice.service.BookStockReadService

@Service
class BookStockReadServiceImpl(
    private val bookStockRepository: BookStockRepository
) : BookStockReadService {

    override fun findByLibrary(libraryId: String): List<BookStock> {
        return bookStockRepository.findByLibraryId(libraryId)
    }

    override fun findByLibraryAndBook(
        libraryId: String,
        bookId: String
    ): BookStock? {
        return bookStockRepository.findByLibraryIdAndBookId(
            libraryId,
            BookId(bookId)
        )
    }

    override fun findByBook(bookId: String): List<BookStock> {
        return bookStockRepository.findByBookId(BookId(bookId))
    }
}
