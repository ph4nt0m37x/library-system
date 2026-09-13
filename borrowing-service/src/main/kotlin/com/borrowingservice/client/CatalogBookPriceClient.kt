package com.borrowingservice.client

import feign.codec.ErrorDecoder
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import java.math.BigDecimal

data class BookPriceResponse(
    val amount: BigDecimal,
    val currency: String
)

@FeignClient(name = "catalog-service", configuration = [CatalogBookPriceClientConfiguration::class])
interface CatalogBookPriceClient {
    @GetMapping("/api/books/{bookId}/price")
    fun findPrice(@PathVariable bookId: String): BookPriceResponse
}

class CatalogBookPriceClientConfiguration {
    @Bean
    fun catalogBookPriceErrorDecoder(): ErrorDecoder = ErrorDecoder { _, response ->
        if (response.status() == 404) {
            CatalogBookNotFoundException()
        } else {
            CatalogServiceUnavailableException()
        }
    }
}

class CatalogBookNotFoundException : RuntimeException("Book was not found in Catalog service")

class CatalogServiceUnavailableException(cause: Throwable? = null) :
    RuntimeException("Catalog service is unavailable", cause)
