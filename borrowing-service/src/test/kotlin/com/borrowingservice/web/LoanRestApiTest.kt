package com.borrowingservice.web

import com.borrowingservice.model.command.CreateLoanCommand
import com.borrowingservice.model.command.DeclareBookLostCommand
import com.borrowingservice.model.command.ExtendLoanCommand
import com.borrowingservice.model.command.RecordPermanentBookDamageCommand
import com.borrowingservice.model.command.ReturnLoanCommand
import com.borrowingservice.model.valueObject.dto.CreateLoanDTO
import com.borrowingservice.service.LoanService
import com.borrowingservice.service.LoanViewReadService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

@ExtendWith(MockitoExtension::class)
class LoanRestApiTest {
    @Mock
    private lateinit var loanViewReadService: LoanViewReadService

    @Test
    fun `create loan maps the request and returns the created resource`() {
        val loanService = RecordingLoanService()
        val api = LoanRestApi(loanService, loanViewReadService)
        val borrowedAt = ZonedDateTime.parse("2026-09-10T10:00:00Z")

        val response = api.createLoan(CreateLoanDTO("member-1", "book-1", borrowedAt)).join()

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("loan-1", response.body?.id)
        assertEquals("/api/loans/loan-1", response.headers.location.toString())

        val command = checkNotNull(loanService.createdLoan)
        assertEquals("member-1", command.memberId)
        assertEquals("book-1", command.bookId)
        assertEquals(borrowedAt, command.borrowedAt)
    }

    private class RecordingLoanService : LoanService {
        var createdLoan: CreateLoanCommand? = null

        override fun createLoan(command: CreateLoanCommand): CompletableFuture<String> {
            createdLoan = command
            return CompletableFuture.completedFuture("loan-1")
        }

        override fun extendLoan(command: ExtendLoanCommand): CompletableFuture<String> = error("Not used")

        override fun returnLoan(command: ReturnLoanCommand): CompletableFuture<String> = error("Not used")

        override fun declareBookLost(command: DeclareBookLostCommand): CompletableFuture<String> = error("Not used")

        override fun recordPermanentBookDamage(
            command: RecordPermanentBookDamageCommand
        ): CompletableFuture<String> = error("Not used")
    }
}
