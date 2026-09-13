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

    fun acceptTransfer(command: AcceptTransferCommand): CompletableFuture<Void>

    fun rejectTransfer(command: RejectTransferCommand): CompletableFuture<Void>

    fun cancelTransfer(command: CancelTransferCommand): CompletableFuture<Void>

    fun shipTransfer(command: ShipTransferCommand): CompletableFuture<Void>

    fun completeTransfer(command: CompleteTransferCommand): CompletableFuture<Void>
}
