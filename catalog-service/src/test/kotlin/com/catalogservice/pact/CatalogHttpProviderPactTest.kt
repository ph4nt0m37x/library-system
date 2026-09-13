package com.catalogservice.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.catalogservice.model.valueObject.BookId
import com.catalogservice.model.valueObject.dto.BookPriceResponseDTO
import com.catalogservice.service.BookViewReadService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal

@Provider("catalog-http-provider")
@PactFolder("pacts")
@ActiveProfiles("pact")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CatalogHttpProviderPactTest {
    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var bookViewReadService: BookViewReadService

    @BeforeEach
    fun setTarget(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun verifyPact(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @State("book $BOOK_ID is priced at 12.35 USD")
    fun pricedBook() {
        `when`(bookViewReadService.getBookPrice(BOOK_ID)).thenReturn(
            BookPriceResponseDTO(BigDecimal("12.35"), "USD")
        )
    }

    @State("book $MISSING_BOOK_ID does not exist or is deleted")
    fun missingBook() {
        `when`(bookViewReadService.getBookPrice(MISSING_BOOK_ID)).thenReturn(null)
    }

    @State("book $BOOK_ID is active")
    fun activeBook() {
        `when`(bookViewReadService.isAvailable(BookId(BOOK_ID))).thenReturn(true)
    }

    @State("book $BOOK_ID is deleted")
    fun deletedBook() {
        `when`(bookViewReadService.isAvailable(BookId(BOOK_ID))).thenReturn(false)
    }

    private companion object {
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        const val MISSING_BOOK_ID = "22222222-2222-2222-2222-222222222223"
    }
}
