package com.inventoryservice.service.impl

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.service.TransferService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class TransferServiceImpl(
    private val commandGateway: CommandGateway
) : TransferService {

    override fun requestTransfer(
        command: RequestTransferCommand
    ): CompletableFuture<TransferId> =
        commandGateway.send(command)

    override fun acceptTransfer(
        command: AcceptTransferCommand
    ): CompletableFuture<TransferId> =
        commandGateway.send(command)

    override fun rejectTransfer(
        command: RejectTransferCommand
    ): CompletableFuture<TransferId> =
        commandGateway.send(command)

    override fun cancelTransfer(
        command: CancelTransferCommand
    ): CompletableFuture<TransferId> =
        commandGateway.send(command)

    override fun shipTransfer(
        command: ShipTransferCommand
    ): CompletableFuture<TransferId> =
        commandGateway.send(command)

    override fun completeTransfer(
        command: CompleteTransferCommand
    ): CompletableFuture<TransferId> =
        commandGateway.send(command)
}