package com.borrowingservice.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.annotations.Pact
import com.borrowingservice.client.CatalogBookNotFoundException
import com.borrowingservice.client.CatalogBookPriceClient
import com.borrowingservice.client.CatalogBookPriceClientConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "catalog-http-provider", pactVersion = PactSpecVersion.V3)
class CatalogPriceFeignConsumerPactTest {

    @Pact(consumer = "borrowing-http-consumer")
    fun pricedBook(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("book $BOOK_ID is priced at 12.35 USD")
        .uponReceiving("a request for the replacement price")
        .path("/api/books/$BOOK_ID/price")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(JSON_HEADERS)
        .body(
            PactDslJsonBody()
                .decimalType("amount", BigDecimal("12.35"))
                .stringValue("currency", "USD")
        )
        .toPact()

    @Pact(consumer = "borrowing-http-consumer")
    fun missingBook(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("book $MISSING_BOOK_ID does not exist or is deleted")
        .uponReceiving("a price request for a missing book")
        .path("/api/books/$MISSING_BOOK_ID/price")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(PROBLEM_HEADERS)
        .body(
            PactDslJsonBody()
                .stringValue("title", "Not Found")
                .integerType("status", 404)
                .stringValue("code", "BOOK_NOT_FOUND")
        )
        .toPact()

    @Test
    @PactTestFor(pactMethod = "pricedBook")
    fun `Feign decodes Catalog amount and currency`(mockServer: MockServer) {
        val response = client(mockServer).findPrice(BOOK_ID)

        assertEquals(BigDecimal("12.35"), response.amount)
        assertEquals("USD", response.currency)
    }

    @Test
    @PactTestFor(pactMethod = "missingBook")
    fun `Feign maps Catalog 404`(mockServer: MockServer) {
        assertThrows(CatalogBookNotFoundException::class.java) {
            client(mockServer).findPrice(MISSING_BOOK_ID)
        }
    }

    private fun client(mockServer: MockServer): CatalogBookPriceClient = pactFeignClient(
        CatalogBookPriceClient::class.java,
        mockServer.getUrl(),
        CatalogBookPriceClientConfiguration().catalogBookPriceErrorDecoder()
    )

    private companion object {
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        const val MISSING_BOOK_ID = "22222222-2222-2222-2222-222222222223"
        val JSON_HEADERS = mapOf("Content-Type" to "application/json")
        val PROBLEM_HEADERS = mapOf("Content-Type" to "application/problem+json")
    }
}
