package com.catalogservice.model.aggregate

import com.catalogservice.model.command.CreateBookCommand
import com.catalogservice.model.command.DeleteBookCommand
import com.catalogservice.model.command.UpdateBookCommand
import com.catalogservice.model.common.Identifier
import com.catalogservice.model.common.LabeledEntity
import com.catalogservice.model.event.BookCreatedEvent
import com.catalogservice.model.event.BookDeletedEvent
import com.catalogservice.model.event.BookUpdatedEvent
import com.catalogservice.model.entity.BookCategory
import com.catalogservice.model.valueObject.BookId
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

@Table(name = "book")
@Aggregate(repository = "axonBookRepository")
@Entity
class Book() : LabeledEntity {

    @AggregateIdentifier
    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "id"))
    private lateinit var id: BookId

    // MUTABLE

    private lateinit var isbn: String
    private lateinit var title: String
    private lateinit var author: String
    private var description: String? = null
    private var publicationYear: Int? = null

    @ManyToOne
    @JoinColumn(name = "category_id")
    private var category: BookCategory? = null

    private var deleted: Boolean = false


    // CREATE

        @CommandHandler
    constructor(command: CreateBookCommand) : this() {

        val event = BookCreatedEvent(
            id = BookId(),
            isbn = command.isbn,
            title = command.title,
            author = command.author,
            description = command.description,
            publicationYear = command.publicationYear,
            category = command.category
        )

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookCreatedEvent) {
        this.id = event.id
        this.isbn = event.isbn
        this.title = event.title
        this.author = event.author
        this.description = event.description
        this.publicationYear = event.publicationYear
        this.category = event.category
        this.deleted = false
    }

    // UPDATE

    @CommandHandler
    fun update(command: UpdateBookCommand) {

        val event = BookUpdatedEvent(
            id = command.id,
            isbn = command.isbn,
            title = command.title,
            author = command.author,
            description = command.description,
            publicationYear = command.publicationYear,
            category = command.category
        )

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookUpdatedEvent) {
        this.isbn = event.isbn
        this.title = event.title
        this.author = event.author
        this.description = event.description
        this.publicationYear = event.publicationYear
        this.category = event.category
    }

    // DELETE

    @CommandHandler
    fun delete(command: DeleteBookCommand) {

        val event = BookDeletedEvent(
            id = this.id
        )

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: BookDeletedEvent) {
        this.deleted = true
    }

    // ENTITY

    override fun getId(): Identifier<out Any> {
        return this.id
    }

    override fun getLabel(): String {
        return "Book $title"
    }

    fun isDeleted(): Boolean {
        return deleted
    }
}
