package com.borrowingservice.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.annotations.Pact
import com.borrowingservice.client.InventoryAvailabilityClient
import com.borrowingservice.client.InventoryAvailabilityClientConfiguration
import com.borrowingservice.client.InventoryResourceNotFoundException
import com.borrowingservice.client.InventoryServiceUnavailableException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "inventory-http-provider", pactVersion = PactSpecVersion.V3)
class InventoryAvailabilityFeignConsumerPactTest {

    @Pact(consumer = "borrowing-http-consumer")
    fun availableCopy(builder: PactDslWithProvider): RequestResponsePact = availabilityPact(
        builder,
        state = "library $LIBRARY_ID has an available copy of book $BOOK_ID",
        description = "a request for an available copy",
        libraryActive = true,
        bookActive = true,
        availableQuantity = 2,
        available = true
    )

    @Pact(consumer = "borrowing-http-consumer")
    fun inactiveLibrary(builder: PactDslWithProvider): RequestResponsePact = availabilityPact(
        builder,
        state = "library $LIBRARY_ID is inactive",
        description = "a request for stock at an inactive library",
        libraryActive = false,
        bookActive = true,
        availableQuantity = 2,
        available = false
    )

    @Pact(consumer = "borrowing-http-consumer")
    fun inactiveBook(builder: PactDslWithProvider): RequestResponsePact = availabilityPact(
        builder,
        state = "book $BOOK_ID is inactive in Catalog",
        description = "a request for stock of an inactive book",
        libraryActive = true,
        bookActive = false,
        availableQuantity = 2,
        available = false
    )

    @Pact(consumer = "borrowing-http-consumer")
    fun noAvailableCopy(builder: PactDslWithProvider): RequestResponsePact = availabilityPact(
        builder,
        state = "library $LIBRARY_ID has no available copy of book $BOOK_ID",
        description = "a request for unavailable stock",
        libraryActive = true,
        bookActive = true,
        availableQuantity = 0,
        available = false
    )

    @Pact(consumer = "borrowing-http-consumer")
    fun missingInventoryResource(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("library $MISSING_LIBRARY_ID does not exist")
        .uponReceiving("a request for stock at a missing library")
        .path("/api/stock/$MISSING_LIBRARY_ID/$BOOK_ID/availability")
        .method("GET")
        .willRespondWith()
        .status(404)
        .headers(PROBLEM_HEADERS)
        .body(problemBody(404, "Not Found", "RESOURCE_NOT_FOUND"))
        .toPact()

    @Pact(consumer = "borrowing-http-consumer")
    fun catalogUnavailable(builder: PactDslWithProvider): RequestResponsePact = builder
        .given("Catalog is unavailable while checking book $BOOK_ID")
        .uponReceiving("a request for stock while Catalog is unavailable")
        .path("/api/stock/$LIBRARY_ID/$BOOK_ID/availability")
        .method("GET")
        .willRespondWith()
        .status(503)
        .headers(PROBLEM_HEADERS)
        .body(problemBody(503, "Service Unavailable", "CATALOG_UNAVAILABLE"))
        .toPact()

    @Test
    @PactTestFor(pactMethod = "availableCopy")
    fun `Feign decodes available stock`(mockServer: MockServer) {
        val response = client(mockServer).availability(LIBRARY_ID, BOOK_ID)

        assertEquals(LIBRARY_ID, response.libraryId)
        assertEquals(BOOK_ID, response.bookId)
        assertTrue(response.libraryActive)
        assertTrue(response.bookActive)
        assertEquals(2, response.availableQuantity)
        assertTrue(response.available)
    }

    @Test
    @PactTestFor(pactMethod = "inactiveLibrary")
    fun `Feign decodes an inactive library decision`(mockServer: MockServer) {
        val response = client(mockServer).availability(LIBRARY_ID, BOOK_ID)
        assertFalse(response.libraryActive)
        assertFalse(response.available)
    }

    @Test
    @PactTestFor(pactMethod = "inactiveBook")
    fun `Feign decodes an inactive book decision`(mockServer: MockServer) {
        val response = client(mockServer).availability(LIBRARY_ID, BOOK_ID)
        assertFalse(response.bookActive)
        assertFalse(response.available)
    }

    @Test
    @PactTestFor(pactMethod = "noAvailableCopy")
    fun `Feign decodes zero stock`(mockServer: MockServer) {
        val response = client(mockServer).availability(LIBRARY_ID, BOOK_ID)
        assertEquals(0, response.availableQuantity)
        assertFalse(response.available)
    }

    @Test
    @PactTestFor(pactMethod = "missingInventoryResource")
    fun `Feign maps Inventory 404`(mockServer: MockServer) {
        assertThrows(InventoryResourceNotFoundException::class.java) {
            client(mockServer).availability(MISSING_LIBRARY_ID, BOOK_ID)
        }
    }

    @Test
    @PactTestFor(pactMethod = "catalogUnavailable")
    fun `Feign maps Inventory dependency failure`(mockServer: MockServer) {
        assertThrows(InventoryServiceUnavailableException::class.java) {
            client(mockServer).availability(LIBRARY_ID, BOOK_ID)
        }
    }

    private fun availabilityPact(
        builder: PactDslWithProvider,
        state: String,
        description: String,
        libraryActive: Boolean,
        bookActive: Boolean,
        availableQuantity: Int,
        available: Boolean
    ): RequestResponsePact = builder
        .given(state)
        .uponReceiving(description)
        .path("/api/stock/$LIBRARY_ID/$BOOK_ID/availability")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(JSON_HEADERS)
        .body(
            PactDslJsonBody()
                .stringValue("libraryId", LIBRARY_ID)
                .stringValue("bookId", BOOK_ID)
                .booleanValue("libraryActive", libraryActive)
                .booleanValue("bookActive", bookActive)
                .integerType("availableQuantity", availableQuantity)
                .booleanValue("available", available)
        )
        .toPact()

    private fun problemBody(status: Int, title: String, code: String) = PactDslJsonBody()
        .stringValue("title", title)
        .integerType("status", status)
        .stringType("detail", "Provider state detail")
        .stringValue("code", code)

    private fun client(mockServer: MockServer): InventoryAvailabilityClient = pactFeignClient(
        InventoryAvailabilityClient::class.java,
        mockServer.getUrl(),
        InventoryAvailabilityClientConfiguration().inventoryAvailabilityErrorDecoder()
    )

    private companion object {
        const val LIBRARY_ID = "33333333-3333-3333-3333-333333333333"
        const val MISSING_LIBRARY_ID = "33333333-3333-3333-3333-333333333334"
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        val JSON_HEADERS = mapOf("Content-Type" to "application/json")
        val PROBLEM_HEADERS = mapOf("Content-Type" to "application/problem+json")
    }
}
