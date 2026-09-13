package com.mcpservice.client

import com.mcpservice.client.dto.BookStockWire
import com.mcpservice.client.dto.LibraryWire
import com.mcpservice.client.dto.StockAvailabilityWire
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "inventory-service")
interface InventoryClient {
    @GetMapping("/api/stock/{libraryId}/{bookId}/availability")
    fun getAvailability(
        @PathVariable("libraryId") libraryId: String,
        @PathVariable("bookId") bookId: String
    ): StockAvailabilityWire

    @GetMapping("/api/stock/book/{bookId}")
    fun findAcrossLibraries(@PathVariable("bookId") bookId: String): List<BookStockWire>

    @GetMapping("/api/libraries/{id}")
    fun findLibrary(@PathVariable("id") id: String): LibraryWire
}
