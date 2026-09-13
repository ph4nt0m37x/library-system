package com.mcpservice.client

import com.mcpservice.client.dto.BorrowingBanWire
import com.mcpservice.client.dto.CommandResponseWire
import com.mcpservice.client.dto.CreateLoanWire
import com.mcpservice.client.dto.FeeWire
import com.mcpservice.client.dto.LoanWire
import com.mcpservice.client.dto.PaymentQuoteWire
import com.mcpservice.client.dto.PaymentWire
import com.mcpservice.client.dto.QuotePaymentWire
import com.mcpservice.client.dto.RecordPaymentWire
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "borrowing-service")
interface BorrowingClient {
    @GetMapping("/api/loans/{id}")
    fun findLoan(@PathVariable("id") id: String): LoanWire

    @GetMapping("/api/loans/member/{memberId}/active")
    fun findActiveLoans(@PathVariable("memberId") memberId: String): List<LoanWire>

    @GetMapping("/api/fees/member/{memberId}/unpaid")
    fun findUnpaidFees(@PathVariable("memberId") memberId: String): List<FeeWire>

    @GetMapping("/api/payments/member/{memberId}")
    fun findPayments(@PathVariable("memberId") memberId: String): List<PaymentWire>

    @GetMapping("/api/borrowing-bans/member/{memberId}")
    fun findBan(@PathVariable("memberId") memberId: String): BorrowingBanWire

    @PostMapping("/api/loans/create")
    fun createLoan(@RequestBody request: CreateLoanWire): CommandResponseWire

    @PostMapping("/api/loans/{id}/extend")
    fun extendLoan(@PathVariable("id") id: String): CommandResponseWire

    @PostMapping("/api/loans/{id}/return")
    fun returnLoan(@PathVariable("id") id: String): CommandResponseWire

    @PostMapping("/api/loans/{id}/lost")
    fun reportLost(@PathVariable("id") id: String): CommandResponseWire

    @PostMapping("/api/loans/{id}/damage")
    fun reportDamage(@PathVariable("id") id: String): CommandResponseWire

    @PostMapping("/api/payments/quote")
    fun quotePayment(@RequestBody request: QuotePaymentWire): PaymentQuoteWire

    @PostMapping("/api/payments/record")
    fun recordPayment(@RequestBody request: RecordPaymentWire): CommandResponseWire
}
