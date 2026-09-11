package com.inventoryservice.model.event.transfer


import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.valueObject.TransferId

data class TransferCompletedEvent(
    override val id: TransferId
) : TransferEvent(id) {

    constructor(command: CompleteTransferCommand) : this(
        id = command.id
    )
}