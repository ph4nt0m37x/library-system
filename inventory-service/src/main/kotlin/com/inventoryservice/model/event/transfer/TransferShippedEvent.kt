package com.inventoryservice.model.event.transfer


import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.valueObject.TransferId

data class TransferShippedEvent(
    override val id: TransferId
) : TransferEvent(id) {

    constructor(command: ShipTransferCommand) : this(
        id = command.id
    )
}