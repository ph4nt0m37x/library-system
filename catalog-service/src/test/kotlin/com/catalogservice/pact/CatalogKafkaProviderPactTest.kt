package com.catalogservice.pact

import au.com.dius.pact.provider.MessageAndMetadata
import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.catalogservice.handler.eventHandler.EventMessagingEventHandler
import com.catalogservice.infrastructure.kafka.CatalogIntegrationOutbox
import com.catalogservice.infrastructure.kafka.CatalogOutboxEvent
import com.catalogservice.model.event.BookDeletedEvent
import com.catalogservice.model.valueObject.BookId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Instant

@Provider("catalog-kafka-provider")
@Consumer("inventory-kafka-consumer")
@PactFolder("pacts")
class CatalogKafkaProviderPactTest {
    private val objectMapper = pactObjectMapper()

    @BeforeEach
    fun setTarget(context: PactVerificationContext) {
        context.target = MessageTestTarget()
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun verifyPact(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @PactVerifyProvider("a book.deleted integration event")
    fun bookDeleted(): MessageAndMetadata {
        val outbox = mock(CatalogIntegrationOutbox::class.java)
        EventMessagingEventHandler(outbox, objectMapper).on(
            BookDeletedEvent(BookId(BOOK_ID)),
            aggregateVersion = 2,
            occurredAt = Instant.parse("2026-09-12T10:00:00Z")
        )

        val captor = ArgumentCaptor.forClass(CatalogOutboxEvent::class.java)
        verify(outbox).save(captor.capture())
        val event = captor.value

        return MessageAndMetadata(
            event.payload.toByteArray(Charsets.UTF_8),
            mapOf(
                "contentType" to "application/json",
                "key" to event.recordKey,
                "topic" to event.topic
            )
        )
    }

    private companion object {
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
    }
}
