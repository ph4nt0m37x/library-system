package com.borrowingservice.handlers.eventHandlers

import com.borrowingservice.model.event.LoanCreatedEvent
import com.borrowingservice.model.event.LoanExtendedEvent
import com.borrowingservice.model.event.LoanMarkedDamagedEvent
import com.borrowingservice.model.event.LoanMarkedLostEvent
import com.borrowingservice.model.event.LoanReturnedEvent
import com.borrowingservice.model.valueObject.enums.LoanStatus
import com.borrowingservice.model.view.LoanView
import com.borrowingservice.repository.LoanViewRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ProcessingGroup("borrowing-projections")
class LoanEventHandler(
    private val loanViewRepository: LoanViewRepository
) {
    @EventHandler
    @Transactional
    fun on(event: LoanCreatedEvent) {
        if (loanViewRepository.existsById(event.loanId)) {
            return
        }

        loanViewRepository.save(
            LoanView(
                loanId = event.loanId,
                memberId = event.memberId,
                bookId = event.bookId,
                libraryId = event.libraryId,
                idempotencyKey = event.idempotencyKey,
                borrowedAt = event.borrowedAt,
                dueAt = event.dueAt,
                status = LoanStatus.ACTIVE
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: LoanExtendedEvent) {
        val view = loanView(event.loanId)
        view.extendedAt = event.extendedAt
        view.dueAt = event.dueAt
        loanViewRepository.save(view)
    }

    @EventHandler
    @Transactional
    fun on(event: LoanReturnedEvent) {
        val view = loanView(event.loanId)
        view.returnedAt = event.returnedAt
        view.status = LoanStatus.RETURNED
        loanViewRepository.save(view)
    }

    @EventHandler
    @Transactional
    fun on(event: LoanMarkedLostEvent) {
        val view = loanView(event.loanId)
        view.incidentDeclaredAt = event.declaredLostAt
        view.status = LoanStatus.LOST
        loanViewRepository.save(view)
    }

    @EventHandler
    @Transactional
    fun on(event: LoanMarkedDamagedEvent) {
        val view = loanView(event.loanId)
        view.incidentDeclaredAt = event.damageRecordedAt
        view.status = LoanStatus.DAMAGED
        loanViewRepository.save(view)
    }

    private fun loanView(loanId: String): LoanView = loanViewRepository.findById(loanId)
        .orElseThrow { IllegalStateException("Loan projection not found: $loanId") }
}
