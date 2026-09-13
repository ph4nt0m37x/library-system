package com.mcpservice.client

import com.mcpservice.client.dto.BookPriceWire
import com.mcpservice.client.dto.CatalogBookWire
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(name = "catalog-service")
interface CatalogClient {
    @GetMapping("/api/books/available")
    fun findAvailable(): List<CatalogBookWire>

    @GetMapping("/api/books/{id}")
    fun findById(@PathVariable("id") id: String): CatalogBookWire

    @GetMapping("/api/books/search/title")
    fun searchByTitle(@RequestParam("title") title: String): List<CatalogBookWire>

    @GetMapping("/api/books/search/author")
    fun searchByAuthor(@RequestParam("author") author: String): List<CatalogBookWire>

    @GetMapping("/api/books/filter/category")
    fun filterByCategory(@RequestParam("categoryId") categoryId: Long): List<CatalogBookWire>

    @GetMapping("/api/books/{bookId}/price")
    fun getPrice(@PathVariable("bookId") bookId: String): BookPriceWire
}
