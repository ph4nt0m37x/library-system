package com.inventoryservice.repository

import com.inventoryservice.model.entity.ProcessedLoanEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import com.inventoryservice.model.entity.LoanInventoryState

@Repository
interface ProcessedLoanEventRepository : JpaRepository<ProcessedLoanEvent, Long> {

    fun existsByEventId(eventId: String): Boolean

    fun existsByLoanIdAndEventType(loanId: String, eventType: String): Boolean
}

@Repository
interface LoanInventoryStateRepository : JpaRepository<LoanInventoryState, String>
