package com.borrowingservice.repository

import com.borrowingservice.model.aggregate.BorrowingBanRecord
import com.borrowingservice.model.aggregate.Fee
import com.borrowingservice.model.aggregate.Loan
import com.borrowingservice.model.aggregate.Payment
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.model.valueObject.enums.FeeStatus
import com.borrowingservice.model.valueObject.enums.LoanStatus
import com.borrowingservice.model.view.BorrowingBanRecordView
import com.borrowingservice.model.view.FeeView
import com.borrowingservice.model.view.LoanView
import com.borrowingservice.model.view.PaymentView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LoanRepository : JpaRepository<Loan, String> {
    fun findByIdempotencyKey(idempotencyKey: String): Loan?

    fun countByMemberIdAndStatus(memberId: String, status: LoanStatus): Long
}

interface FeeRepository : JpaRepository<Fee, String> {
    fun findByLoanId(loanId: String): Fee?

    fun countByMemberIdAndStatus(memberId: String, status: FeeStatus): Long

    @Query(
        """
        select count(f)
        from Fee f
        join Loan l on l.loanId = f.loanId
        where l.memberId = :memberId and f.reason = :reason
        """
    )
    fun countByLoanMemberIdAndReason(
        @Param("memberId") memberId: String,
        @Param("reason") reason: FeeReason
    ): Long
}

interface PaymentRepository : JpaRepository<Payment, String>

interface BorrowingBanRecordRepository : JpaRepository<BorrowingBanRecord, String> {
    fun findByMemberId(memberId: String): BorrowingBanRecord?
}

interface LoanViewRepository : JpaRepository<LoanView, String> {
    fun findByMemberIdOrderByBorrowedAtDesc(memberId: String): List<LoanView>

    fun findByMemberIdAndStatusOrderByBorrowedAtDesc(memberId: String, status: LoanStatus): List<LoanView>
}

interface FeeViewRepository : JpaRepository<FeeView, String> {
    fun findByMemberIdOrderByCreatedAtDesc(memberId: String): List<FeeView>

    fun findByMemberIdAndStatusOrderByCreatedAtDesc(memberId: String, status: FeeStatus): List<FeeView>
}

interface PaymentViewRepository : JpaRepository<PaymentView, String> {
    fun findByMemberIdOrderByPaidAtDesc(memberId: String): List<PaymentView>
}

interface BorrowingBanRecordViewRepository : JpaRepository<BorrowingBanRecordView, String> {
    fun findByMemberId(memberId: String): BorrowingBanRecordView?
}
