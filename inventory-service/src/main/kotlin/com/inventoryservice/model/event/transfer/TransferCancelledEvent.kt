package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.valueObject.TransferId

data class TransferCancelledEvent(
    override val id: TransferId
) : TransferEvent(id) {

    constructor(command: CancelTransferCommand) : this(
        id = command.id
    )
}