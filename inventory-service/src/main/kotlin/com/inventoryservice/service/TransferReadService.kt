package com.inventoryservice.service

import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.model.valueObject.TransferStatus
import com.inventoryservice.model.view.TransferView

interface TransferReadService {

    fun findById(id: TransferId): TransferView?

    fun findAll(
        status: TransferStatus? = null,
        sourceLibraryId: LibraryId? = null,
        destinationLibraryId: LibraryId? = null,
        requestedBy: String? = null
    ): List<TransferView>
}
