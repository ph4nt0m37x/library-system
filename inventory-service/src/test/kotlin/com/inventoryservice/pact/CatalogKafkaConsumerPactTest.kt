package com.inventoryservice.pact

import au.com.dius.pact.consumer.MessagePactBuilder
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.consumer.junit5.ProviderType
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.messaging.Message
import au.com.dius.pact.core.model.messaging.MessagePact
import com.inventoryservice.infrastructure.KafkaEventConsumer
import com.inventoryservice.infrastructure.kafka.InventoryTopics
import com.inventoryservice.infrastructure.kafka.LoanEventTranslator
import com.inventoryservice.model.valueObject.dto.BookDeletedIntegrationEventDTO
import com.inventoryservice.service.CatalogBookRetirementService
import com.inventoryservice.service.LibraryService
import com.inventoryservice.service.LoanEventInboxService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import java.util.UUID

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(
    providerName = "catalog-kafka-provider",
    providerType = ProviderType.ASYNCH,
    pactVersion = PactSpecVersion.V3
)
class CatalogKafkaConsumerPactTest {
    private val objectMapper = pactObjectMapper()
    private lateinit var retirementService: CatalogBookRetirementService
    private lateinit var consumer: KafkaEventConsumer

    @BeforeEach
    fun setUp() {
        retirementService = mock(CatalogBookRetirementService::class.java)
        consumer = KafkaEventConsumer(
            objectMapper = objectMapper,
            loanEventTranslator = LoanEventTranslator(),
            libraryService = mock(LibraryService::class.java),
            loanEventInboxService = mock(LoanEventInboxService::class.java),
            catalogBookRetirementService = retirementService
        )
    }

    @Pact(consumer = "inventory-kafka-consumer")
    fun bookDeleted(builder: MessagePactBuilder): MessagePact = builder
        .expectsToReceive("a book.deleted integration event")
        .withMetadata(
            mapOf(
                "contentType" to "application/json",
                "key" to BOOK_ID,
                "topic" to InventoryTopics.BOOK_DELETED
            )
        )
        .withContent(
            PactDslJsonBody()
                .uuid("eventId", UUID.fromString(EVENT_ID))
                .stringValue("bookId", BOOK_ID)
                .numberValue("eventVersion", 1)
                .integerType("aggregateVersion", 2)
                .stringMatcher("occurredAt", INSTANT_PATTERN, "2026-09-12T10:00:00Z")
        )
        .toPact()

    @Test
    @PactTestFor(pactMethod = "bookDeleted", providerType = ProviderType.ASYNCH)
    fun `listener accepts book deleted`(messages: List<Message>) {
        val message = messages.single()
        consumer.handleBookDeleted(
            ConsumerRecord(
                InventoryTopics.BOOK_DELETED,
                0,
                0,
                message.metadata.getValue("key").toString(),
                message.contentsAsString()
            )
        )

        val invocation = mockingDetails(retirementService).invocations.single { it.method.name == "process" }
        val event = invocation.arguments[0] as BookDeletedIntegrationEventDTO
        assertEquals(EVENT_ID, event.eventId)
        assertEquals(BOOK_ID, event.bookId)
        assertEquals(1, event.eventVersion)
        assertEquals(2, event.aggregateVersion)
    }

    private companion object {
        const val EVENT_ID = "66666666-6666-6666-6666-666666666666"
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        const val INSTANT_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z$"
    }
}
