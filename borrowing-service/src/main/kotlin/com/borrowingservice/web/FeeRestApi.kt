package com.borrowingservice.web

import com.borrowingservice.model.valueObject.dto.FeeResponse
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.service.FeeViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fees")
class FeeRestApi(
    private val feeViewReadService: FeeViewReadService
) {
    @Operation(summary = "Get all fees")
    @GetMapping("/all")
    fun findAllFees(): ResponseEntity<List<FeeResponse>> =
        ResponseEntity.ok(feeViewReadService.findAll())

    @Operation(summary = "Get fee by ID")
    @GetMapping("/{id}")
    fun findFeeById(@PathVariable id: String): ResponseEntity<FeeResponse> =
        feeViewReadService.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: throw ResourceNotFoundException("Fee", id)

    @Operation(summary = "Get all fees for a member")
    @GetMapping("/member/{memberId}")
    fun findFeesByMember(@PathVariable memberId: String): ResponseEntity<List<FeeResponse>> =
        ResponseEntity.ok(feeViewReadService.findByMemberId(memberId))

    @Operation(summary = "Get unpaid fees for a member")
    @GetMapping("/member/{memberId}/unpaid")
    fun findUnpaidFeesByMember(@PathVariable memberId: String): ResponseEntity<List<FeeResponse>> =
        ResponseEntity.ok(feeViewReadService.findUnpaidByMemberId(memberId))
}
