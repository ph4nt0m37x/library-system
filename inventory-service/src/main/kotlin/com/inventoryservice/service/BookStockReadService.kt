package com.inventoryservice.service

import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.valueObject.dto.StockAvailabilityResponse

interface BookStockReadService {

    fun findByLibrary(libraryId: String): List<BookStock>

    fun findByLibraryAndBook(
        libraryId: String,
        bookId: String
    ): BookStock?

    fun findByBook(bookId: String): List<BookStock>

    fun availability(libraryId: String, bookId: String): StockAvailabilityResponse
}

