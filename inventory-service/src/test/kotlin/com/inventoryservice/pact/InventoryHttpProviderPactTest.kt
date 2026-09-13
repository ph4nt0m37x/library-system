package com.inventoryservice.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.inventoryservice.model.exception.DependencyUnavailableException
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.valueObject.dto.StockAvailabilityResponse
import com.inventoryservice.service.BookStockReadService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@Provider("inventory-http-provider")
@PactFolder("pacts")
@ActiveProfiles("pact")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class InventoryHttpProviderPactTest {
    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var bookStockReadService: BookStockReadService

    @BeforeEach
    fun setTarget(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun verifyPact(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @State("library $LIBRARY_ID has an available copy of book $BOOK_ID")
    fun availableCopy() = availability(libraryActive = true, bookActive = true, quantity = 2, available = true)

    @State("library $LIBRARY_ID is inactive")
    fun inactiveLibrary() = availability(libraryActive = false, bookActive = true, quantity = 2, available = false)

    @State("book $BOOK_ID is inactive in Catalog")
    fun inactiveBook() = availability(libraryActive = true, bookActive = false, quantity = 2, available = false)

    @State("library $LIBRARY_ID has no available copy of book $BOOK_ID")
    fun noAvailableCopy() = availability(libraryActive = true, bookActive = true, quantity = 0, available = false)

    @State("library $MISSING_LIBRARY_ID does not exist")
    fun missingLibrary() {
        `when`(bookStockReadService.availability(MISSING_LIBRARY_ID, BOOK_ID)).thenThrow(
            ResourceNotFoundException("Library '$MISSING_LIBRARY_ID' does not exist")
        )
    }

    @State("Catalog is unavailable while checking book $BOOK_ID")
    fun catalogUnavailable() {
        `when`(bookStockReadService.availability(LIBRARY_ID, BOOK_ID)).thenThrow(
            DependencyUnavailableException("Catalog")
        )
    }

    private fun availability(
        libraryActive: Boolean,
        bookActive: Boolean,
        quantity: Int,
        available: Boolean
    ) {
        `when`(bookStockReadService.availability(LIBRARY_ID, BOOK_ID)).thenReturn(
            StockAvailabilityResponse(
                libraryId = LIBRARY_ID,
                bookId = BOOK_ID,
                libraryActive = libraryActive,
                bookActive = bookActive,
                availableQuantity = quantity,
                available = available
            )
        )
    }

    private companion object {
        const val LIBRARY_ID = "33333333-3333-3333-3333-333333333333"
        const val MISSING_LIBRARY_ID = "33333333-3333-3333-3333-333333333334"
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
    }
}
