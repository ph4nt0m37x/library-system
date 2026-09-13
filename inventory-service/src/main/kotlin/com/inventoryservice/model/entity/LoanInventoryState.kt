package com.inventoryservice.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

enum class InventoryLoanStatus {
    BORROWED,
    RETURNED,
    LOST,
    DAMAGED
}

@Entity
@Table(name = "loan_inventory_state")
class LoanInventoryState(
    @Id
    @Column(name = "loan_id", length = 100, nullable = false, updatable = false)
    var loanId: String,

    @Column(name = "library_id", length = 100, nullable = false, updatable = false)
    var libraryId: String,

    @Column(name = "book_id", length = 100, nullable = false, updatable = false)
    var bookId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: InventoryLoanStatus,

    @Column(name = "last_aggregate_version", nullable = false)
    var lastAggregateVersion: Long
)
