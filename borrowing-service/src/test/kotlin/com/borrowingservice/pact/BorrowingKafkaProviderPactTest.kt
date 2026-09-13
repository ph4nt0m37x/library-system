package com.borrowingservice.pact

import au.com.dius.pact.provider.MessageAndMetadata
import au.com.dius.pact.provider.PactVerifyProvider
import au.com.dius.pact.provider.junit5.MessageTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Consumer
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.borrowingservice.integration.IntegrationOutboxEvent
import com.borrowingservice.integration.IntegrationOutboxRepository
import com.borrowingservice.integration.LoanIntegrationOutboxHandler
import com.borrowingservice.model.event.LoanCreatedEvent
import com.borrowingservice.model.event.LoanMarkedDamagedEvent
import com.borrowingservice.model.event.LoanMarkedLostEvent
import com.borrowingservice.model.event.LoanReturnedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Provider("borrowing-kafka-provider")
@Consumer("inventory-kafka-consumer")
@PactFolder("pacts")
class BorrowingKafkaProviderPactTest {
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

    @PactVerifyProvider("a loan.created integration event")
    fun loanCreated(): MessageAndMetadata = messageFromHandler { handler ->
        handler.on(
            LoanCreatedEvent(
                loanId = LOAN_ID,
                memberId = MEMBER_ID,
                bookId = BOOK_ID,
                libraryId = LIBRARY_ID,
                borrowedAt = BORROWED_AT,
                dueAt = BORROWED_AT.plusDays(14),
                idempotencyKey = IDEMPOTENCY_KEY
            ),
            aggregateVersion = 0,
            occurredAt = OCCURRED_AT
        )
    }

    @PactVerifyProvider("a loan.returned integration event")
    fun loanReturned(): MessageAndMetadata = messageFromHandler { handler ->
        handler.on(
            LoanReturnedEvent(
                loanId = LOAN_ID,
                memberId = MEMBER_ID,
                bookId = BOOK_ID,
                libraryId = LIBRARY_ID,
                dueAt = BORROWED_AT.plusDays(14),
                returnedAt = RETURNED_AT,
                idempotencyKey = IDEMPOTENCY_KEY
            ),
            aggregateVersion = 1,
            occurredAt = OCCURRED_AT.plusSeconds(60)
        )
    }

    @PactVerifyProvider("a loan.marked.lost integration event")
    fun loanMarkedLost(): MessageAndMetadata = messageFromHandler { handler ->
        handler.on(
            LoanMarkedLostEvent(
                loanId = LOAN_ID,
                memberId = MEMBER_ID,
                bookId = BOOK_ID,
                libraryId = LIBRARY_ID,
                declaredLostAt = LOST_AT,
                idempotencyKey = IDEMPOTENCY_KEY
            ),
            aggregateVersion = 1,
            occurredAt = OCCURRED_AT.plusSeconds(120)
        )
    }

    @PactVerifyProvider("a loan.marked.damaged integration event")
    fun loanMarkedDamaged(): MessageAndMetadata = messageFromHandler { handler ->
        handler.on(
            LoanMarkedDamagedEvent(
                loanId = LOAN_ID,
                memberId = MEMBER_ID,
                bookId = BOOK_ID,
                libraryId = LIBRARY_ID,
                damageRecordedAt = DAMAGED_AT,
                idempotencyKey = IDEMPOTENCY_KEY
            ),
            aggregateVersion = 1,
            occurredAt = OCCURRED_AT.plusSeconds(180)
        )
    }

    private fun messageFromHandler(invoke: (LoanIntegrationOutboxHandler) -> Unit): MessageAndMetadata {
        val repository = mock(IntegrationOutboxRepository::class.java)
        val handler = LoanIntegrationOutboxHandler(repository, objectMapper)
        invoke(handler)

        val captor = ArgumentCaptor.forClass(IntegrationOutboxEvent::class.java)
        verify(repository).save(captor.capture())
        val outbox = captor.value

        return MessageAndMetadata(
            outbox.payload.toByteArray(Charsets.UTF_8),
            mapOf(
                "contentType" to "application/json",
                "key" to outbox.recordKey,
                "topic" to outbox.topic
            )
        )
    }

    private companion object {
        const val MEMBER_ID = "11111111-1111-1111-1111-111111111111"
        const val BOOK_ID = "22222222-2222-2222-2222-222222222222"
        const val LIBRARY_ID = "33333333-3333-3333-3333-333333333333"
        const val LOAN_ID = "44444444-4444-4444-4444-444444444444"
        const val IDEMPOTENCY_KEY = "checkout-44444444"
        val OCCURRED_AT: Instant = Instant.parse("2026-09-12T10:00:00Z")
        val BORROWED_AT: ZonedDateTime = ZonedDateTime.ofInstant(OCCURRED_AT, ZoneOffset.UTC)
        val RETURNED_AT: ZonedDateTime = BORROWED_AT.plusDays(10)
        val LOST_AT: ZonedDateTime = BORROWED_AT.plusDays(5)
        val DAMAGED_AT: ZonedDateTime = BORROWED_AT.plusDays(6)
    }
}
