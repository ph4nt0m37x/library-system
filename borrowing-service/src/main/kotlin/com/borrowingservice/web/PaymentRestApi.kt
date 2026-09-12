package com.borrowingservice.web

import com.borrowingservice.model.command.RecordPaymentCommand
import com.borrowingservice.model.valueObject.dto.FeeQuoteResponse
import com.borrowingservice.model.valueObject.dto.PaymentQuoteResponse
import com.borrowingservice.model.valueObject.dto.PaymentResponse
import com.borrowingservice.model.valueObject.dto.QuotePaymentDTO
import com.borrowingservice.model.valueObject.dto.RecordPaymentDTO
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.service.PaymentService
import com.borrowingservice.service.PaymentViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/payments")
class PaymentRestApi(
    private val paymentService: PaymentService,
    private val paymentViewReadService: PaymentViewReadService
) {
    @Operation(summary = "Get all payments")
    @GetMapping("/all")
    fun findAllPayments(): ResponseEntity<List<PaymentResponse>> =
        ResponseEntity.ok(paymentViewReadService.findAll())

    @Operation(summary = "Get payment by ID")
    @GetMapping("/{id}")
    fun findPaymentById(@PathVariable id: String): ResponseEntity<PaymentResponse> =
        paymentViewReadService.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: throw ResourceNotFoundException("Payment", id)

    @Operation(summary = "Get all payments for a member")
    @GetMapping("/member/{memberId}")
    fun findPaymentsByMember(@PathVariable memberId: String): ResponseEntity<List<PaymentResponse>> =
        ResponseEntity.ok(paymentViewReadService.findByMemberId(memberId))

    @Operation(summary = "Quote a payment for selected fees")
    @PostMapping("/quote")
    fun quotePayment(@RequestBody dto: QuotePaymentDTO): ResponseEntity<PaymentQuoteResponse> {
        val quote = paymentService.quote(
            paymentId = UUID.randomUUID().toString(),
            memberId = dto.memberId,
            feeIds = dto.feeIds,
            currency = dto.currency,
            quotedAt = dto.quotedAt
        )
        return ResponseEntity.ok(
            PaymentQuoteResponse(
                paymentId = quote.paymentId,
                memberId = quote.memberId,
                currency = quote.currency,
                amount = quote.amount,
                quotedAt = quote.quotedAt,
                fees = quote.fees.map {
                    FeeQuoteResponse(
                        feeId = it.feeId,
                        allocationId = it.allocationId,
                        amount = it.amount
                    )
                }
            )
        )
    }

    @Operation(summary = "Record a payment")
    @PostMapping("/record")
    fun recordPayment(
        @RequestBody dto: RecordPaymentDTO
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        paymentService.recordPayment(
            RecordPaymentCommand(
                paymentId = dto.paymentId,
                memberId = dto.memberId,
                amount = dto.amount,
                currency = dto.currency,
                paidAt = dto.paidAt,
                feeIds = dto.feeIds
            )
        ).thenApply { created ->
            if (created) {
                ResponseEntity.created(URI.create("/api/payments/${dto.paymentId}"))
                    .body(CommandResponse(dto.paymentId))
            } else {
                ResponseEntity.ok(CommandResponse(dto.paymentId))
            }
        }
}
