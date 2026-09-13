package com.catalogservice.model.event

import java.time.Instant

data class BookDeletedExternalEvent(
    val eventId: String,
    val bookId: String,
    val eventVersion: Int = 1,
    val aggregateVersion: Long,
    val occurredAt: Instant
)
