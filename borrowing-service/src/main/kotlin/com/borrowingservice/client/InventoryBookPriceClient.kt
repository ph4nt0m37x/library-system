package com.borrowingservice.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.math.BigDecimal

data class BookPriceResponse(
    val amount: BigDecimal,
    val currency: String
)

@FeignClient(name = "inventory-service")
interface InventoryBookPriceClient {
    // The inventory service must expose this contract before replacement quotes can be used.
    @GetMapping("/api/books/{bookId}/price")
    fun findPrice(@PathVariable bookId: String): BookPriceResponse
}
