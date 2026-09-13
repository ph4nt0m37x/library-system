package com.inventoryservice.infrastructure.kafka

object InventoryTopics {
    const val LOAN_CREATED = "loan.created"
    const val LOAN_RETURNED = "loan.returned"
    const val LOAN_MARKED_LOST = "loan.marked.lost"
    const val LOAN_MARKED_DAMAGED = "loan.marked.damaged"
    const val BOOK_DELETED = "book.deleted"
}
