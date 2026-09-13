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
import com.inventoryservice.model.valueObject.dto.LoanCreatedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedDamagedEventDTO
import com.inventoryservice.model.valueObject.dto.LoanMarkedLostEventDTO
import com.inventoryservice.model.valueObject.dto.LoanReturnedEventDTO
import com.inventoryservice.service.CatalogBookRetirementService
import com.inventoryservice.service.LibraryService
import com.inventoryservice.service.LoanEventInboxService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import java.util.UUID

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(
    providerName = "borrowing-kafka-provider",
    providerType = ProviderType.ASYNCH,
    pactVersion = PactSpecVersion.V3
)
class BorrowingKafkaConsumerPactTest {
    private val objectMapper = pactObjectMapper()
    private lateinit var inboxService: LoanEventInboxService
    private lateinit var consumer: KafkaEventConsumer

    @BeforeEach
    fun setUp() {
        inboxService = mock(LoanEventInboxService::class.java)
        consumer = KafkaEventConsumer(
            objectMapper = objectMapper,
            loanEventTranslator = LoanEventTranslator(),
            libraryService = mock(LibraryService::class.java),
            loanEventInboxService = inboxService,
            catalogBookRetirementService = mock(CatalogBookRetirementService::class.java)
        )
    }

    @Pact(consumer = "inventory-kafka-consumer")
    fun loanCreated(builder: MessagePactBuilder): MessagePact = builder
        .expectsToReceive("a loan.created integration event")
        .withMetadata(metadata(InventoryTopics.LOAN_CREATED))
        .withContent(loanBody("borrowedAt", aggregateVersion = 0))
        .toPact()

    @Pact(consumer = "inventory-kafka-consumer")
    fun loanReturned(builder: MessagePactBuilder): MessagePact = builder
        .expectsToReceive("a loan.returned integration event")
        .withMetadata(metadata(InventoryTopics.LOAN_RETURNED))
        .withContent(loanBody("returnedAt", aggregateVersion = 1))
        .toPact()

    @Pact(consumer = "inventory-kafka-consumer")
    fun loanMarkedLost(builder: MessagePactBuilder): MessagePact = builder
        .expectsToReceive("a loan.marked.lost integration event")
        .withMetadata(metadata(InventoryTopics.LOAN_MARKED_LOST))
        .withContent(loanBody("declaredLostAt", aggregateVersion = 1))
        .toPact()

    @Pact(consumer = "inventory-kafka-consumer")
    fun loanMarkedDamaged(builder: MessagePactBuilder): MessagePact = builder
        .expectsToReceive("a loan.marked.damaged integration event")
        .withMetadata(metadata(InventoryTopics.LOAN_MARKED_DAMAGED))
        .withContent(loanBody("damageRecordedAt", aggregateVersion = 1))
        .toPact()

    @Test
    @PactTestFor(pactMethod = "loanCreated", providerType = ProviderType.ASYNCH)
    fun `listener accepts loan created`(messages: List<Message>) {
        consumer.handleLoanCreated(record(InventoryTopics.LOAN_CREATED, messages.single()))
        assertAccepted(InventoryTopics.LOAN_CREATED, LoanCreatedEventDTO::class.java, 0)
    }

    @Test
    @PactTestFor(pactMethod = "loanReturned", providerType = ProviderType.ASYNCH)
    fun `listener accepts loan returned`(messages: List<Message>) {
        consumer.handleLoanReturned(record(InventoryTopics.LOAN_RETURNED, messages.single()))
        assertAccepted(InventoryTopics.LOAN_RETURNED, LoanReturnedEventDTO::class.java, 1)
    }

    @Test
    @PactTestFor(pactMethod = "loanMarkedLost", providerType = ProviderType.ASYNCH)
    fun `listener accepts loan marked lost`(messages: List<Message>) {
        consumer.handleLoanMarkedLost(record(InventoryTopics.LOAN_MARKED_LOST, messages.single()))
        assertAccepted(InventoryTopics.LOAN_MARKED_LOST, LoanMarkedLostEventDTO::class.java, 1)
    }

    @Test
    @PactTestFor(pactMethod = "loanMarkedDamaged", providerType = ProviderType.ASYNCH)
    fun `listener accepts loan marked damaged`(messages: List<Message>) {
        consumer.handleLoanMarkedDamaged(record(InventoryTopics.LOAN_MARKED_DAMAGED, messages.single()))
        assertAccepted(InventoryTopics.LOAN_MARKED_DAMAGED, LoanMarkedDamagedEventDTO::class.java, 1)
    }

    private fun loanBody(transitionTimestamp: String, aggregateVersion: Int): PactDslJsonBody =
        PactDslJsonBody()
            .uuid("eventId", UUID.fromString(EVENT_ID))
            .stringValue("loanId", LOAN_ID)
            .numberValue("eventVersion", 1)
            .integerType("aggregateVersion", aggregateVersion)
            .stringMatcher("occurredAt", INSTANT_PATTERN, "2026-09-12T10:00:00Z")
            .stringValue("libraryId", LIBRARY_ID)
            .stringValue("bookId", BOOK_ID)
            .stringValue("idempotencyKey", IDEMPOTENCY_KEY)
            .stringMatcher(transitionTimestamp, INSTANT_PATTERN, "2026-09-12T10:00:00Z")

    private fun metadata(topic: String): Map<String, Any> = mapOf(
        "contentType" to "application/json",
        "key" to LOAN_ID,
        "topic" to topic
    )

    private fun record(topic: String, message: Message): ConsumerRecord<String, String> = ConsumerRecord(
        topic,
        0,
        0,
        message.metadata.getValue("key").toString(),
        message.contentsAsString()
    )

    private fun assertAccepted(eventType: String, eventClass: Class<*>, aggregateVersion: Long) {
        val invocation = mockingDetails(inboxService).invocations.single { it.method.name == "process" }
        assertEquals(eventType, invocation.arguments[0])

        val event = invocation.arguments[1] as LoanEventDTO
        assertInstanceOf(eventClass, event)
        assertEquals(EVENT_ID, event.eventId)
        assertEquals(LOAN_ID, event.loanId)
        assertEquals(BOOK_ID, event.bookId)
        assertEquals(LIBRARY_ID, event.libraryId)
        assertEquals(1, event.eventVersion)
        assertEquals(aggregateVersion, event.aggregateVersion)
        assertEquals(IDEMPOTENCY_KEY, event.idempotencyKey)
    }

    private companion object {
        const val EVENT_ID = "55555555-5555-5555-5555-555555555555"
        const val LOAN_ID = "44444444-4444-4444-4444-444444444444"
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        const val LIBRARY_ID = "33333333-3333-3333-3333-333333333333"
        const val IDEMPOTENCY_KEY = "checkout-44444444"
        const val INSTANT_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?Z$"
    }
}
