package com.inventoryservice.repository

import com.inventoryservice.model.entity.ProcessedLoanEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ProcessedLoanEventRepository : JpaRepository<ProcessedLoanEvent, Long> {

    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            INSERT IGNORE INTO inventory_event_inbox
                (event_id, loan_id, idempotency_key, event_type, event_version, occurred_at, processed_at)
            VALUES
                (:eventId, :loanId, :idempotencyKey, :eventType, :eventVersion, :occurredAt, CURRENT_TIMESTAMP(6))
        """,
        nativeQuery = true
    )
    fun insertIfAbsent(
        @Param("eventId") eventId: String,
        @Param("loanId") loanId: String,
        @Param("idempotencyKey") idempotencyKey: String?,
        @Param("eventType") eventType: String,
        @Param("eventVersion") eventVersion: Int,
        @Param("occurredAt") occurredAt: Instant
    ): Int
}
