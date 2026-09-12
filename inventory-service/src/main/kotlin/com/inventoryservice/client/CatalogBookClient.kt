package com.inventoryservice.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "catalog-service")
interface CatalogBookClient {

    @GetMapping("/api/books/{bookId}/available")
    fun isBookAvailable(@PathVariable bookId: String): Boolean
}
