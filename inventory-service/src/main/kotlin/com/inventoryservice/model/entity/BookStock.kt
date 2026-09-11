package com.inventoryservice.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "book_stock",
    uniqueConstraints = [
        UniqueConstraint(
            columnNames = ["library_id", "book_id"]
        )
    ]
)
class BookStock(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "library_id", nullable = false)
    val libraryId: String,

    @Column(name = "book_id", nullable = false)
    val bookId: String,

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = 0,

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int = 0
) {
    val borrowedQuantity: Int
        get() = totalQuantity - availableQuantity
}