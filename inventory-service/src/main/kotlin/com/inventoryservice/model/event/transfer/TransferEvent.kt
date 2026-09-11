package com.inventoryservice.model.event.transfer

import com.inventoryservice.model.event.AbstractEvent
import com.inventoryservice.model.valueObject.TransferId

abstract class TransferEvent(
    open val id: TransferId
) : AbstractEvent(id)