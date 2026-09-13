package com.inventoryservice.service.impl

import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.model.valueObject.TransferStatus
import com.inventoryservice.model.view.TransferView
import com.inventoryservice.repository.TransferRepository
import com.inventoryservice.service.TransferReadService
import org.springframework.stereotype.Service

@Service
class TransferReadServiceImpl(
    private val transferRepository: TransferRepository
) : TransferReadService {

    override fun findById(id: TransferId): TransferView? =
        transferRepository.findById(id).orElse(null)?.let { TransferView.from(it) }

    override fun findAll(
        status: TransferStatus?,
        sourceLibraryId: LibraryId?,
        destinationLibraryId: LibraryId?,
        requestedBy: String?
    ): List<TransferView> =
        transferRepository.findAll()
            .asSequence()
            .filter { status == null || it.status() == status }
            .filter { sourceLibraryId == null || it.sourceLibraryId() == sourceLibraryId }
            .filter { destinationLibraryId == null || it.destinationLibraryId() == destinationLibraryId }
            .filter { requestedBy == null || it.requestedBy() == requestedBy }
            .map { TransferView.from(it) }
            .toList()
}
