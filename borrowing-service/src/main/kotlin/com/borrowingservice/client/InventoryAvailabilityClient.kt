package com.borrowingservice.client

import feign.codec.ErrorDecoder
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

data class StockAvailabilityResponse(
    val libraryId: String,
    val bookId: String,
    val libraryActive: Boolean,
    val bookActive: Boolean,
    val availableQuantity: Int,
    val available: Boolean
)

@FeignClient(name = "inventory-service", configuration = [InventoryAvailabilityClientConfiguration::class])
interface InventoryAvailabilityClient {
    @GetMapping("/api/stock/{libraryId}/{bookId}/availability")
    fun availability(
        @PathVariable libraryId: String,
        @PathVariable bookId: String
    ): StockAvailabilityResponse
}

class InventoryAvailabilityClientConfiguration {
    @Bean
    fun inventoryAvailabilityErrorDecoder(): ErrorDecoder = ErrorDecoder { _, response ->
        if (response.status() == 404) {
            InventoryResourceNotFoundException()
        } else {
            InventoryServiceUnavailableException()
        }
    }
}

class InventoryResourceNotFoundException :
    RuntimeException("Library or book was not found in Inventory service")

class InventoryServiceUnavailableException(cause: Throwable? = null) :
    RuntimeException("Inventory service is unavailable", cause)
