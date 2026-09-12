package com.borrowingservice.web

import com.borrowingservice.model.valueObject.dto.BorrowingBanResponse
import com.borrowingservice.model.valueObject.ResourceNotFoundException
import com.borrowingservice.service.BorrowingBanViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/borrowing-bans")
class BorrowingBanRestApi(
    private val borrowingBanViewReadService: BorrowingBanViewReadService
) {
    @Operation(summary = "Get all borrowing-ban records")
    @GetMapping("/all")
    fun findAllBorrowingBans(): ResponseEntity<List<BorrowingBanResponse>> =
        ResponseEntity.ok(borrowingBanViewReadService.findAll())

    @Operation(summary = "Get borrowing-ban record by ID")
    @GetMapping("/{id}")
    fun findBorrowingBanById(@PathVariable id: String): ResponseEntity<BorrowingBanResponse> =
        borrowingBanViewReadService.findById(id)
            ?.let { ResponseEntity.ok(it) }
            ?: throw ResourceNotFoundException("Borrowing ban", id)

    @Operation(summary = "Get borrowing-ban record for a member")
    @GetMapping("/member/{memberId}")
    fun findBorrowingBanByMember(@PathVariable memberId: String): ResponseEntity<BorrowingBanResponse> =
        borrowingBanViewReadService.findByMemberId(memberId)
            ?.let { ResponseEntity.ok(it) }
            ?: throw ResourceNotFoundException("Borrowing ban for member", memberId)
}
