package com.inventoryservice.model.entity

import com.inventoryservice.model.exception.InvalidStockQuantityException
import com.inventoryservice.model.valueObject.BookId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Check

@Entity
@Check(
    constraints = "library_id <> '' AND book_id <> '' " +
        "AND total_quantity >= 0 AND total_quantity <= 1000000 " +
        "AND available_quantity >= 0 AND available_quantity <= total_quantity"
)
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

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "book_id", nullable = false))
    val bookId: BookId,

    @Column(name = "total_quantity", nullable = false)
    var totalQuantity: Int = 0,

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Int = 0
) {
    val borrowedQuantity: Int
        get() = totalQuantity - availableQuantity

    init {
        validateState(totalQuantity, availableQuantity)
    }

    fun validateChange(totalChange: Int, availableChange: Int) {
        changedState(totalChange, availableChange)
    }

    fun applyChange(totalChange: Int, availableChange: Int) {
        val (newTotal, newAvailable) = changedState(totalChange, availableChange)
        totalQuantity = newTotal
        availableQuantity = newAvailable
    }

    private fun changedState(totalChange: Int, availableChange: Int): Pair<Int, Int> {
        validateState(totalQuantity, availableQuantity)
        return validatedState(
            checkedAdd(totalQuantity, totalChange),
            checkedAdd(availableQuantity, availableChange)
        )
    }

    companion object {
        const val MAX_QUANTITY = 1_000_000

        fun validateChangeQuantity(quantity: Int) {
            if (quantity !in 1..MAX_QUANTITY) {
                throw InvalidStockQuantityException(
                    "Quantity must be between 1 and $MAX_QUANTITY"
                )
            }
        }

        fun validateState(totalQuantity: Int, availableQuantity: Int) {
            if (totalQuantity !in 0..MAX_QUANTITY) {
                throw InvalidStockQuantityException(
                    "Total quantity must be between 0 and $MAX_QUANTITY"
                )
            }
            if (availableQuantity < 0 || availableQuantity > totalQuantity) {
                throw InvalidStockQuantityException(
                    "Available quantity must be between 0 and total quantity"
                )
            }
        }

        private fun validatedState(totalQuantity: Int, availableQuantity: Int): Pair<Int, Int> {
            validateState(totalQuantity, availableQuantity)
            return totalQuantity to availableQuantity
        }

        private fun checkedAdd(current: Int, change: Int): Int =
            try {
                Math.addExact(current, change)
            } catch (_: ArithmeticException) {
                throw InvalidStockQuantityException("Stock quantity is too large")
            }

    }
}
