package com.inventoryservice.model.aggregate

import com.inventoryservice.model.common.Identifier
import com.inventoryservice.model.common.LabeledEntity
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.LibraryAddress
import com.inventoryservice.model.valueObject.LibraryName
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.command.library.BorrowBookStockCommand
import com.inventoryservice.model.command.library.ReturnBookStockCommand
import com.inventoryservice.model.event.library.LibraryCreatedEvent
import com.inventoryservice.model.event.library.LibraryDeletedEvent
import com.inventoryservice.model.event.library.LibraryUpdatedEvent
import com.inventoryservice.model.event.library.BookStockIncreasedEvent
import com.inventoryservice.model.event.library.BookStockDecreasedEvent
import com.inventoryservice.model.event.library.BookStockBorrowedEvent
import com.inventoryservice.model.event.library.BookStockReturnedEvent
import com.inventoryservice.model.command.library.MarkBookStockLostCommand
import com.inventoryservice.model.event.library.BookStockMarkedLostEvent
import com.inventoryservice.model.exception.DomainConflictException
import com.inventoryservice.model.exception.ResourceNotFoundException
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Table(
    name = "library",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_library_name_address",
            columnNames = ["name", "address"]
        )
    ]
)
@Aggregate(repository = "axonLibraryRepository")
@Entity
class Library() : LabeledEntity {

    @AggregateIdentifier
    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "id"))
    private lateinit var id: LibraryId

    // MUTABLE

    @Column(name = "name", nullable = false, length = 200)
    private lateinit var name: String

    @Column(name = "address", nullable = false, length = 500)
    private lateinit var address: String

    private var deleted: Boolean = false

    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    @JoinColumn(
        name = "library_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false
    )
    private val stock: MutableList<BookStock> = mutableListOf()


    // CREATE

    @CommandHandler
    constructor(command: CreateLibraryCommand) : this() {

        val name = LibraryName.of(command.name)
        val address = LibraryAddress.of(command.address)

        val event = LibraryCreatedEvent(
            id = LibraryId(),
            name = name.value,
            address = address.value
        )

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: LibraryCreatedEvent) {
        this.id = event.id
        this.name = event.name
        this.address = event.address
        this.deleted = false
    }

    // UPDATE

    @CommandHandler
    fun update(command: UpdateLibraryCommand) {

        val name = LibraryName.of(command.name)
        val address = LibraryAddress.of(command.address)

        val event = LibraryUpdatedEvent(
            id = command.id,
            name = name.value,
            address = address.value
        )

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: LibraryUpdatedEvent) {
        this.name = event.name
        this.address = event.address
    }


    // DELETE

    @CommandHandler
    fun delete(command: DeleteLibraryCommand) {

        val event = LibraryDeletedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: LibraryDeletedEvent) {
        this.deleted = true
    }


    // ADD BOOK STOCK
    // Physically adds new copies to the library.

    @CommandHandler
    fun increaseStock(command: AddBookStockCommand) {

        BookStock.validateChangeQuantity(command.quantity)

        val existingStock = stock.find {
            it.bookId == command.bookId
        }

        if (existingStock != null) {
            existingStock.validateChange(command.quantity, command.quantity)
        } else {
            BookStock.validateState(command.quantity, command.quantity)
        }

        val event = BookStockIncreasedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockIncreasedEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        if (existingStock != null) {
            existingStock.applyChange(event.quantity, event.quantity)
        } else {
            stock.add(
                BookStock(
                    libraryId = this.id.value,
                    bookId = event.bookId,
                    totalQuantity = event.quantity,
                    availableQuantity = event.quantity
                )
            )
        }
    }


    // REMOVE BOOK STOCK
    // Physically removes copies from the library.

    @CommandHandler
    fun decreaseStock(command: RemoveBookStockCommand) {

        BookStock.validateChangeQuantity(command.quantity)

        val existingStock = stock.find {
            it.bookId == command.bookId
        }

        if (existingStock == null) {
            throw ResourceNotFoundException("Book is not in this library")
        }

        if (existingStock.availableQuantity < command.quantity) {
            throw DomainConflictException("Not enough available copies")
        }

        existingStock.validateChange(-command.quantity, -command.quantity)

        val event = BookStockDecreasedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockDecreasedEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        existingStock?.let {
            it.applyChange(-event.quantity, -event.quantity)
        }
    }


    // BORROW BOOK
    // A copy is borrowed, so only available decreases.

    @CommandHandler
    fun borrowStock(command: BorrowBookStockCommand) {

        BookStock.validateChangeQuantity(command.quantity)

        val existingStock = stock.find {
            it.bookId == command.bookId
        }

        if (existingStock == null) {
            throw ResourceNotFoundException("Book is not in this library")
        }

        if (existingStock.availableQuantity < command.quantity) {
            throw DomainConflictException("Not enough available copies")
        }

        existingStock.validateChange(0, -command.quantity)

        val event = BookStockBorrowedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockBorrowedEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        existingStock?.let {
            it.applyChange(0, -event.quantity)
        }
    }


    // RETURN BOOK
    // A borrowed copy comes back, so available increases.

    @CommandHandler
    fun returnStock(command: ReturnBookStockCommand) {

        BookStock.validateChangeQuantity(command.quantity)

        val existingStock = stock.find {
            it.bookId == command.bookId
        }

        if (existingStock == null) {
            throw ResourceNotFoundException("Book is not in this library")
        }

        try {
            existingStock.validateChange(0, command.quantity)
        } catch (exception: IllegalArgumentException) {
            throw DomainConflictException(exception.message ?: "The book cannot be returned")
        }

        val event = BookStockReturnedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockReturnedEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        existingStock?.let {
            it.applyChange(0, event.quantity)
        }
    }

    @CommandHandler
    fun markStockLost(command: MarkBookStockLostCommand) {

        BookStock.validateChangeQuantity(command.quantity)

        val existingStock = stock.find {
            it.bookId == command.bookId
        }

        if (existingStock == null) {
            throw ResourceNotFoundException("Book is not in this library")
        }

        if (existingStock.totalQuantity - existingStock.availableQuantity < command.quantity) {
            throw DomainConflictException("Not enough borrowed copies to mark as lost")
        }

        existingStock.validateChange(-command.quantity, 0)

        val event = BookStockMarkedLostEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockMarkedLostEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        existingStock?.let {
            it.applyChange(-event.quantity, 0)
        }
    }


    // ENTITY

    override fun getId(): Identifier<out Any> {
        return this.id
    }

    override fun getLabel(): String {
        return "Library $name"
    }

    fun isDeleted(): Boolean {
        return deleted
    }
}
