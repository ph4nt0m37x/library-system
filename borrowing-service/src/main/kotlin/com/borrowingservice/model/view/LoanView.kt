package com.borrowingservice.model.view

import com.borrowingservice.model.valueObject.enums.LoanStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(
    name = "loan_view",
    indexes = [Index(name = "idx_loan_view_member", columnList = "member_id")]
)
class LoanView(
    @Id
    @Column(name = "loan_id", nullable = false, updatable = false)
    var loanId: String,

    @Column(name = "member_id", nullable = false, updatable = false)
    var memberId: String,

    @Column(name = "book_id", nullable = false, updatable = false)
    var bookId: String,

    @Column(name = "library_id", length = 100, updatable = false)
    var libraryId: String? = null,

    @Column(name = "borrowed_at", nullable = false)
    var borrowedAt: ZonedDateTime,

    @Column(name = "due_at", nullable = false)
    var dueAt: ZonedDateTime,

    @Column(name = "idempotency_key", unique = true, length = 100, updatable = false)
    var idempotencyKey: String? = null,

    @Column(name = "extended_at")
    var extendedAt: ZonedDateTime? = null,

    @Column(name = "returned_at")
    var returnedAt: ZonedDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: LoanStatus,

    @Column(name = "incident_declared_at")
    var incidentDeclaredAt: ZonedDateTime? = null
)
