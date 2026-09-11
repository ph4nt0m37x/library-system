package com.inventoryservice.web

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.service.TransferService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transfers")
class TransferRestApi(
    private val transferService: TransferService
) {

    @Operation(
        summary = "Request transfer",
        description = "Request a transfer between libraries."
    )
    @PostMapping("/request")
    fun requestTransfer(
        @RequestBody command: RequestTransferCommand
    ): ResponseEntity<TransferId> =
        ResponseEntity.ok(
            transferService.requestTransfer(command).join()
        )

    @Operation(
        summary = "Accept transfer",
        description = "Accept a requested transfer."
    )
    @PostMapping("/accept")
    fun acceptTransfer(
        @RequestBody command: AcceptTransferCommand
    ): ResponseEntity<TransferId> =
        ResponseEntity.ok(
            transferService.acceptTransfer(command).join()
        )

    @Operation(
        summary = "Reject transfer",
        description = "Reject a requested transfer."
    )
    @PostMapping("/reject")
    fun rejectTransfer(
        @RequestBody command: RejectTransferCommand
    ): ResponseEntity<TransferId> =
        ResponseEntity.ok(
            transferService.rejectTransfer(command).join()
        )

    @Operation(
        summary = "Cancel transfer",
        description = "Cancel an existing transfer."
    )
    @PostMapping("/cancel")
    fun cancelTransfer(
        @RequestBody command: CancelTransferCommand
    ): ResponseEntity<TransferId> =
        ResponseEntity.ok(
            transferService.cancelTransfer(command).join()
        )

    @Operation(
        summary = "Ship transfer",
        description = "Mark a transfer as shipped."
    )
    @PostMapping("/ship")
    fun shipTransfer(
        @RequestBody command: ShipTransferCommand
    ): ResponseEntity<TransferId> =
        ResponseEntity.ok(
            transferService.shipTransfer(command).join()
        )

    @Operation(
        summary = "Complete transfer",
        description = "Mark a transfer as completed."
    )
    @PostMapping("/complete")
    fun completeTransfer(
        @RequestBody command: CompleteTransferCommand
    ): ResponseEntity<TransferId> =
        ResponseEntity.ok(
            transferService.completeTransfer(command).join()
        )
}