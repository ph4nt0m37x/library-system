package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.valueObject.TransferId

data class TransferAcceptedEvent(
    override val id: TransferId,
    val reviewedBy: String
) : TransferEvent(id) {

    constructor(command: AcceptTransferCommand) : this(
        id = command.id,
        reviewedBy = command.reviewedBy
    )
}