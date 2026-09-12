package com.inventoryservice.model.aggregate

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.common.Identifier
import com.inventoryservice.model.common.LabeledEntity
import com.inventoryservice.model.entity.BookStock
import com.inventoryservice.model.event.transfer.TransferAcceptedEvent
import com.inventoryservice.model.event.transfer.TransferCancelledEvent
import com.inventoryservice.model.event.transfer.TransferCompletedEvent
import com.inventoryservice.model.event.transfer.TransferRejectedEvent
import com.inventoryservice.model.event.transfer.TransferRequestedEvent
import com.inventoryservice.model.event.transfer.TransferShippedEvent
import com.inventoryservice.model.exception.DomainConflictException
import com.inventoryservice.model.exception.DomainValidationException
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.BookId
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.model.valueObject.TransferStatus
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Check
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import java.time.Instant

@Table(name = "transfer")
@Check(constraints = "quantity >= 1 AND quantity <= 1000000")
@Aggregate(repository = "axonTransferRepository")
@Entity
class Transfer() : LabeledEntity {

    @AggregateIdentifier
    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "id"))
    private lateinit var id: TransferId

    // MUTABLE

    @AttributeOverride(
        name = "value",
        column = Column(name = "source_library_id")
    )
    private lateinit var sourceLibraryId: LibraryId

    @Embedded
    @AttributeOverride(
        name = "value",
        column = Column(name = "destination_library_id")
    )
    private lateinit var destinationLibraryId: LibraryId
    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "book_id"))
    private lateinit var bookId: BookId

    @Column(name = "quantity", nullable = false)
    private var quantity: Int = 0

    @Enumerated(EnumType.STRING)
    private lateinit var status: TransferStatus

    @Column(name = "requested_by", nullable = false, length = 200)
    private lateinit var requestedBy: String

    @Column(name = "requested_at", nullable = false)
    private lateinit var requestedAt: Instant

    @Column(name = "reviewed_by", length = 200)
    private var reviewedBy: String? = null

    @Column(name = "reviewed_at")
    private var reviewedAt: Instant? = null

    @Column(name = "completed_at")
    private var completedAt: Instant? = null

    @Column(name = "failure_reason", length = 500)
    private var failureReason: String? = null

    @Column(name = "correlation_id", length = 100)
    private var correlationId: String? = null

    @Column(name = "causation_id", length = 100)
    private var causationId: String? = null


    // CREATE

    @CommandHandler
    constructor(command: RequestTransferCommand) : this() {

        BookStock.validateChangeQuantity(command.quantity)

        if (command.sourceLibraryId == command.destinationLibraryId) {
            throw DomainValidationException("Source and destination libraries must be different")
        }
        validateActor(command.requestedBy, "requestedBy")

        val event = TransferRequestedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferRequestedEvent) {
        this.id = event.id
        this.sourceLibraryId = event.sourceLibraryId
        this.destinationLibraryId = event.destinationLibraryId
        this.bookId = event.bookId
        this.quantity = event.quantity
        this.requestedBy = event.requestedBy
        this.requestedAt = event.requestedAt
        this.correlationId = event.correlationId
        this.causationId = event.causationId
        this.failureReason = null
        this.status = TransferStatus.REQUESTED
    }

    // ACCEPT

    @CommandHandler
    fun accept(command: AcceptTransferCommand) {

        if (status != TransferStatus.REQUESTED) {
            throw DomainConflictException("Only requested transfers can be accepted")
        }
        validateActor(command.reviewedBy, "reviewedBy")

        val event = TransferAcceptedEvent(
            command = command,
            sourceLibraryId = sourceLibraryId,
            bookId = bookId,
            quantity = quantity
        )

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferAcceptedEvent) {
        this.status = TransferStatus.ACCEPTED
        this.reviewedBy = event.reviewedBy
        this.reviewedAt = event.reviewedAt
        this.failureReason = null
    }

    // SHIP

    @CommandHandler
    fun ship(command: ShipTransferCommand) {
        if (status != TransferStatus.ACCEPTED) {
            throw DomainConflictException("Only accepted transfers can be shipped")
        }

        val event = TransferShippedEvent(
            command = command,
            sourceLibraryId = sourceLibraryId,
            bookId = bookId,
            quantity = quantity
        )

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferShippedEvent) {
        this.status = TransferStatus.SHIPPED
    }


    // REJECT

    @CommandHandler
    fun reject(command: RejectTransferCommand) {

        if (status != TransferStatus.REQUESTED) {
            throw DomainConflictException("Only requested transfers can be rejected")
        }
        validateActor(command.reviewedBy, "reviewedBy")

        val event = TransferRejectedEvent(command)

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferRejectedEvent) {
        this.status = TransferStatus.REJECTED
        this.reviewedBy = event.reviewedBy
        this.reviewedAt = event.reviewedAt
        this.failureReason = event.reason
    }

    // CANCEL

    @CommandHandler
    fun cancel(command: CancelTransferCommand) {

        if (status != TransferStatus.REQUESTED && status != TransferStatus.ACCEPTED) {
            throw DomainConflictException("Only requested or accepted transfers can be cancelled")
        }

        val event = TransferCancelledEvent(
            command = command,
            sourceLibraryId = sourceLibraryId,
            bookId = bookId,
            quantity = quantity,
            releaseStock = status == TransferStatus.ACCEPTED
        )

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferCancelledEvent) {
        this.status = TransferStatus.CANCELLED
        this.failureReason = event.reason
    }


    // COMPLETE

    @CommandHandler
    fun complete(command: CompleteTransferCommand) {
        if (status != TransferStatus.SHIPPED) {
            throw DomainConflictException("Only shipped transfers can be completed")
        }

        val event = TransferCompletedEvent(
            command = command,
            destinationLibraryId = destinationLibraryId,
            bookId = bookId,
            quantity = quantity
        )

        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferCompletedEvent) {
        this.status = TransferStatus.COMPLETED
        this.completedAt = event.completedAt
    }


    // ENTITY

    override fun getId(): Identifier<out Any> {
        return this.id
    }

    override fun getLabel(): String {
        return "Transfer ${this.id}"
    }

    fun sourceLibraryId(): LibraryId = sourceLibraryId

    fun id(): TransferId = id

    fun destinationLibraryId(): LibraryId = destinationLibraryId

    fun bookId(): BookId = bookId

    fun quantity(): Int = quantity

    fun status(): TransferStatus = status

    fun requestedBy(): String = requestedBy

    fun reviewedBy(): String? = reviewedBy

    fun requestedAt(): Instant = requestedAt

    fun reviewedAt(): Instant? = reviewedAt

    fun completedAt(): Instant? = completedAt

    fun failureReason(): String? = failureReason

    fun correlationId(): String? = correlationId

    fun causationId(): String? = causationId

    private fun validateActor(value: String, field: String) {
        if (value.isBlank()) {
            throw DomainValidationException("$field must not be blank")
        }
        if (value.length > 200) {
            throw DomainValidationException("$field must not exceed 200 characters")
        }
    }
}
