package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.valueObject.TransferId
import java.time.Instant

data class TransferRejectedEvent(
    override val id: TransferId,
    val reviewedBy: String,
    val reviewedAt: Instant,
    val reason: String? = null
) : TransferEvent(id) {

    constructor(command: RejectTransferCommand) : this(
        id = command.id,
        reviewedBy = command.reviewedBy,
        reviewedAt = requireNotNull(command.reviewedAt) {
            "reviewedAt must be set before publishing TransferRejectedEvent"
        },
        reason = command.reason
    )
}
