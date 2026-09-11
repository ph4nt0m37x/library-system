package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.valueObject.TransferId

data class TransferRejectedEvent(
    override val id: TransferId,
    val reviewedBy: String
) : TransferEvent(id) {

    constructor(command: RejectTransferCommand) : this(
        id = command.id,
        reviewedBy = command.reviewedBy
    )
}