package com.inventoryservice.model.aggregate

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.common.Identifier
import com.inventoryservice.model.common.LabeledEntity
import com.inventoryservice.model.event.transfer.TransferAcceptedEvent
import com.inventoryservice.model.event.transfer.TransferCancelledEvent
import com.inventoryservice.model.event.transfer.TransferCompletedEvent
import com.inventoryservice.model.event.transfer.TransferRejectedEvent
import com.inventoryservice.model.event.transfer.TransferRequestedEvent
import com.inventoryservice.model.event.transfer.TransferShippedEvent
import com.inventoryservice.model.valueObject.LibraryId
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
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import java.time.LocalDateTime

@Table(name = "transfer")
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
    private lateinit var bookId: String

    private var quantity: Int = 0

    @Enumerated(EnumType.STRING)
    private lateinit var status: TransferStatus

    private lateinit var requestedBy: String
    private lateinit var requestedAt: LocalDateTime

    private var reviewedBy: String? = null
    private var reviewedAt: LocalDateTime? = null
    private var completedAt: LocalDateTime? = null


    // CREATE

    @CommandHandler
    constructor(command: RequestTransferCommand) : this() {

        require(command.quantity > 0) {
            "Quantity must be greater than zero"
        }

        require(command.sourceLibraryId != command.destinationLibraryId) {
            "Source and destination libraries must be different"
        }

        val event = TransferRequestedEvent(command)

        this.on(event)
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
        this.requestedAt = LocalDateTime.now()
        this.status = TransferStatus.REQUESTED
    }

    // ACCEPT

    @CommandHandler
    fun accept(command: AcceptTransferCommand) {

        require(status == TransferStatus.REQUESTED) {
            "Only requested transfers can be accepted"
        }

        val event = TransferAcceptedEvent(command)

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferAcceptedEvent) {
        this.status = TransferStatus.ACCEPTED
        this.reviewedBy = event.reviewedBy
        this.reviewedAt = LocalDateTime.now()
    }

    // SHIP

    @CommandHandler
    fun ship(command: ShipTransferCommand) {
        require(status == TransferStatus.ACCEPTED) {
            "Only accepted transfers can be shipped"
        }

        val event = TransferShippedEvent(
            command = command,
            sourceLibraryId = sourceLibraryId,
            bookId = bookId,
            quantity = quantity
        )

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferShippedEvent) {
        this.status = TransferStatus.SHIPPED
    }


    // REJECT

    @CommandHandler
    fun reject(command: RejectTransferCommand) {

        require(status == TransferStatus.REQUESTED) {
            "Only requested transfers can be rejected"
        }

        val event = TransferRejectedEvent(command)

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferRejectedEvent) {
        this.status = TransferStatus.REJECTED
        this.reviewedBy = event.reviewedBy
        this.reviewedAt = LocalDateTime.now()
    }

    // CANCEL

    @CommandHandler
    fun cancel(command: CancelTransferCommand) {

        require(
            status == TransferStatus.REQUESTED ||
                    status == TransferStatus.ACCEPTED
        ) {
            "Only requested or accepted transfers can be cancelled"
        }

        val event = TransferCancelledEvent(command)

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferCancelledEvent) {
        this.status = TransferStatus.CANCELLED
    }


    // COMPLETE

    @CommandHandler
    fun complete(command: CompleteTransferCommand) {
        require(status == TransferStatus.SHIPPED) {
            "Only shipped transfers can be completed"
        }

        val event = TransferCompletedEvent(
            command = command,
            destinationLibraryId = destinationLibraryId,
            bookId = bookId,
            quantity = quantity
        )

        this.on(event)
        AggregateLifecycle.apply(event)
    }

    @EventSourcingHandler
    fun on(event: TransferCompletedEvent) {
        this.status = TransferStatus.COMPLETED
        this.completedAt = LocalDateTime.now()
    }


    // ENTITY

    override fun getId(): Identifier<out Any> {
        return this.id
    }

    override fun getLabel(): String {
        return "Transfer ${this.id}"
    }
}