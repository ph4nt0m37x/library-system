package com.borrowingservice.model.aggregate

import com.borrowingservice.handlers.eventSourcingHandlers.LoanEventSourcingHandler
import com.borrowingservice.model.valueObject.enums.LoanStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.spring.stereotype.Aggregate
import java.time.ZonedDateTime

@Aggregate(repository = "axonLoanRepository")
@Entity
@Table(name = "loans")
class Loan : LoanEventSourcingHandler() {
    @AggregateIdentifier
    @Id
    @Column(name = "loan_id", nullable = false, updatable = false)
    override lateinit var loanId: String
        protected set

    @Column(name = "member_id", nullable = false, updatable = false)
    override lateinit var memberId: String
        protected set

    @Column(name = "book_id", nullable = false, updatable = false)
    override lateinit var bookId: String
        protected set

    @Column(name = "idempotency_key", unique = true, length = 100, updatable = false)
    override var idempotencyKey: String? = null
        protected set

    @Column(name = "borrowed_at", nullable = false)
    override lateinit var borrowedAt: ZonedDateTime
        protected set

    @Column(name = "due_at", nullable = false)
    override lateinit var dueAt: ZonedDateTime
        protected set

    @Column(name = "extended_at")
    override var extendedAt: ZonedDateTime? = null
        protected set

    @Column(name = "returned_at")
    override var returnedAt: ZonedDateTime? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    override lateinit var status: LoanStatus
        protected set

    @Column(name = "incident_declared_at")
    override var incidentDeclaredAt: ZonedDateTime? = null
        protected set
}
