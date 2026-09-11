package com.inventoryservice.service

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.valueObject.TransferId
import java.util.concurrent.CompletableFuture

interface TransferService {

    fun requestTransfer(command: RequestTransferCommand): CompletableFuture<TransferId>

    fun acceptTransfer(command: AcceptTransferCommand): CompletableFuture<TransferId>

    fun rejectTransfer(command: RejectTransferCommand): CompletableFuture<TransferId>

    fun cancelTransfer(command: CancelTransferCommand): CompletableFuture<TransferId>

    fun shipTransfer(command: ShipTransferCommand): CompletableFuture<TransferId>

    fun completeTransfer(command: CompleteTransferCommand): CompletableFuture<TransferId>
}