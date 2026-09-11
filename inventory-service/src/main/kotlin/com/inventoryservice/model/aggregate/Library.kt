package com.inventoryservice.model.aggregate

import com.inventoryservice.model.common.Identifier
import com.inventoryservice.model.common.LabeledEntity
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.command.library.CreateLibraryCommand
import com.inventoryservice.model.command.library.DeleteLibraryCommand
import com.inventoryservice.model.command.library.UpdateLibraryCommand
import com.inventoryservice.model.event.library.LibraryCreatedEvent
import com.inventoryservice.model.event.library.LibraryDeletedEvent
import com.inventoryservice.model.event.library.LibraryUpdatedEvent
import com.inventoryservice.model.command.library.AddBookStockCommand
import com.inventoryservice.model.command.library.RemoveBookStockCommand
import com.inventoryservice.model.event.library.BookStockIncreasedEvent
import com.inventoryservice.model.event.library.BookStockDecreasedEvent
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Table(name = "library")
@Aggregate(repository = "axonLibraryRepository")
@Entity
class Library() : LabeledEntity {

    @AggregateIdentifier
    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "id"))
    private lateinit var id: LibraryId

    // MUTABLE

    private lateinit var name: String
    private lateinit var address: String

    private var deleted: Boolean = false

    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    private val stock: MutableList<BookStock> = mutableListOf()


    // CREATE

    @CommandHandler
    constructor(command: CreateLibraryCommand) : this() {

        val event = LibraryCreatedEvent(command)

        this.on(event)
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

        val event = LibraryUpdatedEvent(command)

        this.on(event)
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

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: LibraryDeletedEvent) {
        this.deleted = true
    }


    // ADD BOOK STOCK

    @CommandHandler
    fun increaseStock(command: AddBookStockCommand) {

        require(command.quantity > 0) {
            "Quantity must be greater than zero"
        }

        val event = BookStockIncreasedEvent(command)

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockIncreasedEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        if (existingStock != null) {
            existingStock.quantity += event.quantity
        } else {
            stock.add(
                BookStock(
                    libraryId = this.id.value,
                    bookId = event.bookId,
                    quantity = event.quantity
                )
            )
        }
    }


    // REMOVE BOOK STOCK

    @CommandHandler
    fun decreaseStock(command: RemoveBookStockCommand) {

        require(command.quantity > 0) {
            "Quantity must be greater than zero"
        }

        val existingStock = stock.find {
            it.bookId == command.bookId
        }

        require(existingStock != null) {
            "Book is not in this library"
        }

        require(existingStock.quantity >= command.quantity) {
            "Not enough copies available"
        }

        val event = BookStockDecreasedEvent(command)

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookStockDecreasedEvent) {

        val existingStock = stock.find {
            it.bookId == event.bookId
        }

        existingStock?.let {
            it.quantity -= event.quantity
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