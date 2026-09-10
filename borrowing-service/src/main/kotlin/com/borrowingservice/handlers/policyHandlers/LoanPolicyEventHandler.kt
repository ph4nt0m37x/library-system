package com.borrowingservice.handlers.policyHandlers

import com.borrowingservice.config.FeePolicyConfiguration
import com.borrowingservice.model.command.CreateDamagedBookFeeCommand
import com.borrowingservice.model.command.CreateLostBookFeeCommand
import com.borrowingservice.model.command.CreateOverdueFeeCommand
import com.borrowingservice.model.event.LoanMarkedDamagedEvent
import com.borrowingservice.model.event.LoanMarkedLostEvent
import com.borrowingservice.model.event.LoanReturnedEvent
import com.borrowingservice.repository.FeeRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.ZonedDateTime
import java.util.UUID

@Component
@ProcessingGroup("borrowing-policies")
class LoanPolicyEventHandler(
    private val commandGateway: CommandGateway,
    private val feeRepository: FeeRepository,
    private val feePolicy: FeePolicyConfiguration,
    private val clock: Clock
) {
    @EventHandler
    @Transactional
    fun on(event: LoanReturnedEvent) {
        if (!event.returnedAt.isAfter(event.dueAt) || feeRepository.findByLoanId(event.loanId) != null) {
            return
        }

        commandGateway.sendAndWait<String>(
            CreateOverdueFeeCommand(
                feeId = feeIdFor(event.loanId),
                loanId = event.loanId,
                memberId = event.memberId,
                currency = feePolicy.currency,
                dueAt = event.dueAt,
                returnedAt = event.returnedAt,
                createdAt = now()
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LoanMarkedLostEvent) {
        if (feeRepository.findByLoanId(event.loanId) != null) {
            return
        }

        commandGateway.sendAndWait<String>(
            CreateLostBookFeeCommand(
                feeId = feeIdFor(event.loanId),
                loanId = event.loanId,
                memberId = event.memberId,
                currency = feePolicy.currency,
                declaredLostAt = event.declaredLostAt,
                createdAt = now()
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LoanMarkedDamagedEvent) {
        if (feeRepository.findByLoanId(event.loanId) != null) {
            return
        }

        commandGateway.sendAndWait<String>(
            CreateDamagedBookFeeCommand(
                feeId = feeIdFor(event.loanId),
                loanId = event.loanId,
                memberId = event.memberId,
                currency = feePolicy.currency,
                damageRecordedAt = event.damageRecordedAt,
                createdAt = now()
            )
        )
    }

    private fun feeIdFor(loanId: String): String =
        UUID.nameUUIDFromBytes("fee:loan:$loanId".toByteArray(StandardCharsets.UTF_8)).toString()

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)
}
