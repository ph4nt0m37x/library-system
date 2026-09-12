package com.inventoryservice.web

import com.inventoryservice.model.command.transfer.AcceptTransferCommand
import com.inventoryservice.model.command.transfer.CancelTransferCommand
import com.inventoryservice.model.command.transfer.CompleteTransferCommand
import com.inventoryservice.model.command.transfer.RejectTransferCommand
import com.inventoryservice.model.command.transfer.RequestTransferCommand
import com.inventoryservice.model.command.transfer.ShipTransferCommand
import com.inventoryservice.model.dto.TransferCommandResponse
import com.inventoryservice.model.exception.ResourceNotFoundException
import com.inventoryservice.model.valueObject.LibraryId
import com.inventoryservice.model.valueObject.TransferId
import com.inventoryservice.model.valueObject.TransferStatus
import com.inventoryservice.model.view.TransferView
import com.inventoryservice.service.TransferService
import com.inventoryservice.service.TransferReadService
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/transfers")
class TransferRestApi(
    private val transferService: TransferService,
    private val transferReadService: TransferReadService
) {

    @Operation(
        summary = "Get transfer status",
        description = "Get the current status and workflow details for a transfer."
    )
    @GetMapping("/{id}")
    fun getTransfer(
        @PathVariable id: String
    ): ResponseEntity<TransferView> {
        val transfer = transferReadService.findById(TransferId(id))
            ?: throw ResourceNotFoundException("Transfer '$id' does not exist")

        return ResponseEntity.ok(transfer)
    }

    @Operation(
        summary = "List transfers",
        description = "List transfers, optionally filtered by status, source, destination, or requester."
    )
    @GetMapping
    fun listTransfers(
        @RequestParam(required = false) status: TransferStatus?,
        @RequestParam(required = false) sourceLibraryId: String?,
        @RequestParam(required = false) destinationLibraryId: String?,
        @RequestParam(required = false) requestedBy: String?
    ): ResponseEntity<List<TransferView>> =
        ResponseEntity.ok(
            transferReadService.findAll(
                status = status,
                sourceLibraryId = sourceLibraryId?.let(::LibraryId),
                destinationLibraryId = destinationLibraryId?.let(::LibraryId),
                requestedBy = requestedBy
            )
        )

    @Operation(
        summary = "Request transfer",
        description = "Request a transfer between libraries."
    )
    @PostMapping("/request")
    fun requestTransfer(
        @Valid @RequestBody command: RequestTransferCommand
    ): ResponseEntity<TransferCommandResponse> =
        ResponseEntity.status(HttpStatus.ACCEPTED).body(
            TransferCommandResponse(
                id = transferService.requestTransfer(command).join().baseValue()
            )
        )

    @Operation(
        summary = "Accept transfer",
        description = "Accept a requested transfer and reserve its source stock."
    )
    @PostMapping("/accept")
    fun acceptTransfer(
        @Valid @RequestBody command: AcceptTransferCommand
    ): ResponseEntity<TransferCommandResponse> {
        transferService.acceptTransfer(command).join()
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(TransferCommandResponse(command.id.baseValue()))
    }

    @Operation(
        summary = "Reject transfer",
        description = "Reject a requested transfer."
    )
    @PostMapping("/reject")
    fun rejectTransfer(
        @Valid @RequestBody command: RejectTransferCommand
    ): ResponseEntity<TransferCommandResponse> {
        transferService.rejectTransfer(command).join()
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(TransferCommandResponse(command.id.baseValue()))
    }

    @Operation(
        summary = "Cancel transfer",
        description = "Cancel an existing transfer."
    )
    @PostMapping("/cancel")
    fun cancelTransfer(
        @Valid @RequestBody command: CancelTransferCommand
    ): ResponseEntity<TransferCommandResponse> {
        transferService.cancelTransfer(command).join()
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(TransferCommandResponse(command.id.baseValue()))
    }

    @Operation(
        summary = "Ship transfer",
        description = "Mark a transfer with reserved source stock as shipped."
    )
    @PostMapping("/ship")
    fun shipTransfer(
        @Valid @RequestBody command: ShipTransferCommand
    ): ResponseEntity<TransferCommandResponse> {
        transferService.shipTransfer(command).join()
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(TransferCommandResponse(command.id.baseValue()))
    }

    @Operation(
        summary = "Complete transfer",
        description = "Mark a transfer as completed."
    )
    @PostMapping("/complete")
    fun completeTransfer(
        @Valid @RequestBody command: CompleteTransferCommand
    ): ResponseEntity<TransferCommandResponse> {
        transferService.completeTransfer(command).join()
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(TransferCommandResponse(command.id.baseValue()))
    }
}
