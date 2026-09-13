package com.borrowingservice.integration

import java.time.Instant

object LoanTopics {
    const val CREATED = "loan.created"
    const val RETURNED = "loan.returned"
    const val MARKED_LOST = "loan.marked.lost"
    const val MARKED_DAMAGED = "loan.marked.damaged"
}

interface LoanIntegrationEvent {
    val eventId: String
    val loanId: String
    val eventVersion: Int
    val aggregateVersion: Long
    val occurredAt: Instant
    val libraryId: String
    val bookId: String
    val idempotencyKey: String?
}

data class LoanCreatedIntegrationEvent(
    override val eventId: String,
    override val loanId: String,
    override val eventVersion: Int = 1,
    override val aggregateVersion: Long,
    override val occurredAt: Instant,
    override val libraryId: String,
    override val bookId: String,
    override val idempotencyKey: String?,
    val borrowedAt: Instant
) : LoanIntegrationEvent

data class LoanReturnedIntegrationEvent(
    override val eventId: String,
    override val loanId: String,
    override val eventVersion: Int = 1,
    override val aggregateVersion: Long,
    override val occurredAt: Instant,
    override val libraryId: String,
    override val bookId: String,
    override val idempotencyKey: String?,
    val returnedAt: Instant
) : LoanIntegrationEvent

data class LoanMarkedLostIntegrationEvent(
    override val eventId: String,
    override val loanId: String,
    override val eventVersion: Int = 1,
    override val aggregateVersion: Long,
    override val occurredAt: Instant,
    override val libraryId: String,
    override val bookId: String,
    override val idempotencyKey: String?,
    val declaredLostAt: Instant
) : LoanIntegrationEvent

data class LoanMarkedDamagedIntegrationEvent(
    override val eventId: String,
    override val loanId: String,
    override val eventVersion: Int = 1,
    override val aggregateVersion: Long,
    override val occurredAt: Instant,
    override val libraryId: String,
    override val bookId: String,
    override val idempotencyKey: String?,
    val damageRecordedAt: Instant
) : LoanIntegrationEvent
