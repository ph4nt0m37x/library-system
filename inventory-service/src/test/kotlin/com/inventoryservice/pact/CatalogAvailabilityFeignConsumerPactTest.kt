package com.inventoryservice.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.annotations.Pact
import com.inventoryservice.client.CatalogBookClient
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "catalog-http-provider", pactVersion = PactSpecVersion.V3)
class CatalogAvailabilityFeignConsumerPactTest {

    @Pact(consumer = "inventory-http-consumer")
    fun activeBook(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("book $BOOK_ID is active")
        .uponReceiving("a request to check an active book")
        .path("/api/books/$BOOK_ID/available")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(JSON_HEADERS)
        .body("true")
        .toPact()

    @Pact(consumer = "inventory-http-consumer")
    fun inactiveBook(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("book $BOOK_ID is deleted")
        .uponReceiving("a request to check a deleted book")
        .path("/api/books/$BOOK_ID/available")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(JSON_HEADERS)
        .body("false")
        .toPact()

    @Test
    @PactTestFor(pactMethod = "activeBook")
    fun `Feign decodes an active Catalog book`(mockServer: MockServer) {
        assertTrue(client(mockServer).isBookAvailable(BOOK_ID))
    }

    @Test
    @PactTestFor(pactMethod = "inactiveBook")
    fun `Feign decodes a deleted Catalog book`(mockServer: MockServer) {
        assertFalse(client(mockServer).isBookAvailable(BOOK_ID))
    }

    private fun client(mockServer: MockServer): CatalogBookClient = pactFeignClient(
        CatalogBookClient::class.java,
        mockServer.getUrl()
    )

    private companion object {
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        val JSON_HEADERS = mapOf("Content-Type" to "application/json")
    }
}
