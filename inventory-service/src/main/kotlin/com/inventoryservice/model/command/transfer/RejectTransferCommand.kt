package com.inventoryservice.model.command.transfer

import com.inventoryservice.model.valueObject.TransferId
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.axonframework.modelling.command.TargetAggregateIdentifier
import java.time.Instant

data class RejectTransferCommand(
    @TargetAggregateIdentifier
    val id: TransferId,
    @field:NotBlank
    @field:Size(max = 200)
    val reviewedBy: String,
    val reviewedAt: Instant? = null,
    @field:Size(max = 500)
    val reason: String? = null
)
