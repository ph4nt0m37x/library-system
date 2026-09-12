package com.borrowingservice.handlers.eventSourcingHandlers

import com.borrowingservice.model.event.LoanCreatedEvent
import com.borrowingservice.model.event.LoanExtendedEvent
import com.borrowingservice.model.event.LoanMarkedDamagedEvent
import com.borrowingservice.model.event.LoanMarkedLostEvent
import com.borrowingservice.model.event.LoanReturnedEvent
import com.borrowingservice.model.valueObject.enums.LoanStatus
import org.axonframework.eventsourcing.EventSourcingHandler
import java.time.ZonedDateTime

abstract class LoanEventSourcingHandler {
    abstract var loanId: String
        protected set

    abstract var memberId: String
        protected set

    abstract var bookId: String
        protected set

    abstract var libraryId: String?
        protected set

    abstract var idempotencyKey: String?
        protected set

    abstract var borrowedAt: ZonedDateTime
        protected set

    abstract var dueAt: ZonedDateTime
        protected set

    abstract var extendedAt: ZonedDateTime?
        protected set

    abstract var returnedAt: ZonedDateTime?
        protected set

    abstract var status: LoanStatus
        protected set

    abstract var incidentDeclaredAt: ZonedDateTime?
        protected set

    @EventSourcingHandler
    fun on(event: LoanCreatedEvent) {
        loanId = event.loanId
        memberId = event.memberId
        bookId = event.bookId
        libraryId = event.libraryId
        idempotencyKey = event.idempotencyKey
        borrowedAt = event.borrowedAt
        dueAt = event.dueAt
        status = LoanStatus.ACTIVE
    }

    @EventSourcingHandler
    fun on(event: LoanExtendedEvent) {
        dueAt = event.dueAt
        extendedAt = event.extendedAt
    }

    @EventSourcingHandler
    fun on(event: LoanReturnedEvent) {
        returnedAt = event.returnedAt
        status = LoanStatus.RETURNED
    }

    @EventSourcingHandler
    fun on(event: LoanMarkedLostEvent) {
        incidentDeclaredAt = event.declaredLostAt
        status = LoanStatus.LOST
    }

    @EventSourcingHandler
    fun on(event: LoanMarkedDamagedEvent) {
        incidentDeclaredAt = event.damageRecordedAt
        status = LoanStatus.DAMAGED
    }
}
