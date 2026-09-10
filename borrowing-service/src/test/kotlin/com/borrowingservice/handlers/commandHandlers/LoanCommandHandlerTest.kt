package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.client.MembershipClient
import com.borrowingservice.config.LoanPolicyConfiguration
import com.borrowingservice.model.aggregate.Loan
import com.borrowingservice.model.command.CreateLoanCommand
import com.borrowingservice.model.valueObject.LoanEligibilityException
import com.borrowingservice.model.valueObject.enums.LoanRejectionReason
import com.borrowingservice.model.valueObject.enums.LoanStatus
import com.borrowingservice.repository.BorrowingBanRecordRepository
import com.borrowingservice.repository.FeeRepository
import com.borrowingservice.repository.LoanRepository
import org.axonframework.modelling.command.Repository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.ZoneOffset
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class LoanCommandHandlerTest {
    @Mock
    private lateinit var loanAggregateRepository: Repository<Loan>

    @Mock
    private lateinit var loanRepository: LoanRepository

    @Mock
    private lateinit var feeRepository: FeeRepository

    @Mock
    private lateinit var banRecordRepository: BorrowingBanRecordRepository

    @Mock
    private lateinit var membershipClient: MembershipClient

    private lateinit var handler: LoanCommandHandler

    private val borrowedAt = ZonedDateTime.parse("2026-09-10T10:00:00Z")

    @BeforeEach
    fun setUp() {
        handler = LoanCommandHandler(
            loanAggregateRepository = loanAggregateRepository,
            loanRepository = loanRepository,
            feeRepository = feeRepository,
            banRecordRepository = banRecordRepository,
            membershipClient = membershipClient,
            policy = LoanPolicyConfiguration(
                maxActiveLoans = 5,
                maxUnpaidFees = 3,
                regularLoanDuration = "P14D",
                extensionDuration = "P14D"
            ),
            clock = Clock.fixed(borrowedAt.toInstant(), ZoneOffset.UTC)
        )

        `when`(membershipClient.hasActiveSubscription(MEMBER_ID)).thenReturn(true)
    }

    @Test
    fun `allows the fifth active loan`() {
        `when`(loanRepository.countByMemberIdAndStatus(MEMBER_ID, LoanStatus.ACTIVE)).thenReturn(4)

        val result = handler.handle(createLoanCommand())

        assertEquals(LOAN_ID, result)
        verify(loanAggregateRepository).newInstance(any())
    }

    @Test
    fun `rejects a sixth active loan without creating it`() {
        `when`(loanRepository.countByMemberIdAndStatus(MEMBER_ID, LoanStatus.ACTIVE)).thenReturn(5)

        val exception = assertThrows(LoanEligibilityException::class.java) {
            handler.handle(createLoanCommand())
        }

        assertEquals(LoanRejectionReason.ACTIVE_LOAN_LIMIT_REACHED, exception.reason)
        verifyNoInteractions(loanAggregateRepository)
    }

    private fun createLoanCommand() = CreateLoanCommand(
        loanId = LOAN_ID,
        memberId = MEMBER_ID,
        bookId = "book-1",
        borrowedAt = borrowedAt
    )

    private companion object {
        const val MEMBER_ID = "member-1"
        const val LOAN_ID = "loan-1"
    }
}
