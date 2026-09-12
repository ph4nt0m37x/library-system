package com.inventoryservice.model.view

import com.inventoryservice.model.aggregate.Transfer
import com.inventoryservice.model.valueObject.TransferStatus
import java.time.Instant

/**
 * Read representation of a transfer. It is built from the transfer aggregate
 * table so clients can inspect the workflow without reading aggregate internals.
 */
data class TransferView(
    val id: String,
    val status: TransferStatus,
    val sourceLibraryId: String,
    val destinationLibraryId: String,
    val titleId: String,
    val quantity: Int,
    val requestedBy: String,
    val reviewedBy: String?,
    val requestedAt: Instant,
    val reviewedAt: Instant?,
    val completedAt: Instant?,
    val failureReason: String?,
    val workflowId: String,
    val correlationId: String?,
    val causationId: String?
) {
    companion object {
        fun from(transfer: Transfer): TransferView {
            val workflowId = transfer.id().baseValue()

            return TransferView(
                id = workflowId,
                status = transfer.status(),
                sourceLibraryId = transfer.sourceLibraryId().baseValue(),
                destinationLibraryId = transfer.destinationLibraryId().baseValue(),
                titleId = transfer.bookId().baseValue(),
                quantity = transfer.quantity(),
                requestedBy = transfer.requestedBy(),
                reviewedBy = transfer.reviewedBy(),
                requestedAt = transfer.requestedAt(),
                reviewedAt = transfer.reviewedAt(),
                completedAt = transfer.completedAt(),
                failureReason = transfer.failureReason(),
                workflowId = workflowId,
                correlationId = transfer.correlationId() ?: workflowId,
                causationId = transfer.causationId()
            )
        }
    }
}
