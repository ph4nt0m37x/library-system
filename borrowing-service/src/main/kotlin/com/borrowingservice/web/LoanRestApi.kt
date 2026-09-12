package com.borrowingservice.web

import com.borrowingservice.model.command.CreateLoanCommand
import com.borrowingservice.model.command.DeclareBookLostCommand
import com.borrowingservice.model.command.ExtendLoanCommand
import com.borrowingservice.model.command.RecordPermanentBookDamageCommand
import com.borrowingservice.model.command.ReturnLoanCommand
import com.borrowingservice.model.valueObject.dto.CreateLoanDTO
import com.borrowingservice.model.valueObject.dto.DeclareBookLostDTO
import com.borrowingservice.model.valueObject.dto.ExtendLoanDTO
import com.borrowingservice.model.valueObject.dto.LoanResponse
import com.borrowingservice.model.valueObject.dto.RecordPermanentBookDamageDTO
import com.borrowingservice.model.valueObject.dto.ReturnLoanDTO
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.service.LoanService
import com.borrowingservice.service.LoanViewReadService
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
@RequestMapping("/api/loans")
class LoanRestApi(
    private val loanService: LoanService,
    private val loanViewReadService: LoanViewReadService
) {
    @Operation(summary = "Get all loans")
    @GetMapping("/all")
    fun findAllLoans(): ResponseEntity<List<LoanResponse>> =
        ResponseEntity.ok(loanViewReadService.findAll())

    @Operation(summary = "Get loan by ID")
    @GetMapping("/{id}")
    fun findLoanById(@PathVariable id: String): ResponseEntity<LoanResponse> =
        loanViewReadService.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: throw ResourceNotFoundException("Loan", id)

    @Operation(summary = "Get all loans for a member")
    @GetMapping("/member/{memberId}")
    fun findLoansByMember(@PathVariable memberId: String): ResponseEntity<List<LoanResponse>> =
        ResponseEntity.ok(loanViewReadService.findByMemberId(memberId))

    @Operation(summary = "Get active loans for a member")
    @GetMapping("/member/{memberId}/active")
    fun findActiveLoansByMember(@PathVariable memberId: String): ResponseEntity<List<LoanResponse>> =
        ResponseEntity.ok(loanViewReadService.findActiveByMemberId(memberId))

    @Operation(summary = "Create a loan")
    @PostMapping("/create")
    fun createLoan(@RequestBody dto: CreateLoanDTO): CompletableFuture<ResponseEntity<CommandResponse>> {
        val loanId = UUID.randomUUID().toString()
        return loanService.createLoan(
            CreateLoanCommand(
                loanId = loanId,
                memberId = dto.memberId,
                bookId = dto.bookId,
                borrowedAt = dto.borrowedAt,
                idempotencyKey = dto.idempotencyKey
            )
        ).thenApply { id ->
            ResponseEntity.created(URI.create("/api/loans/$id")).body(CommandResponse(id))
        }
    }

    @Operation(summary = "Extend a loan")
    @PostMapping("/{id}/extend")
    fun extendLoan(
        @PathVariable id: String,
        @RequestBody(required = false) dto: ExtendLoanDTO?
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        loanService.extendLoan(ExtendLoanCommand(id, dto?.extendedAt))
            .thenApply { ResponseEntity.ok(CommandResponse(it)) }

    @Operation(summary = "Return a loan")
    @PostMapping("/{id}/return")
    fun returnLoan(
        @PathVariable id: String,
        @RequestBody(required = false) dto: ReturnLoanDTO?
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        loanService.returnLoan(ReturnLoanCommand(id, dto?.returnedAt))
            .thenApply { ResponseEntity.ok(CommandResponse(it)) }

    @Operation(summary = "Declare a loaned book lost")
    @PostMapping("/{id}/lost")
    fun declareBookLost(
        @PathVariable id: String,
        @RequestBody(required = false) dto: DeclareBookLostDTO?
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        loanService.declareBookLost(DeclareBookLostCommand(id, dto?.declaredLostAt))
            .thenApply { ResponseEntity.ok(CommandResponse(it)) }

    @Operation(summary = "Record permanent damage to a loaned book")
    @PostMapping("/{id}/damage")
    fun recordPermanentBookDamage(
        @PathVariable id: String,
        @RequestBody(required = false) dto: RecordPermanentBookDamageDTO?
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        loanService.recordPermanentBookDamage(RecordPermanentBookDamageCommand(id, dto?.damageRecordedAt))
            .thenApply { ResponseEntity.ok(CommandResponse(it)) }
}
