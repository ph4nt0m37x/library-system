package com.borrowingservice.service.impl

import com.borrowingservice.model.valueObject.dto.BanPeriodResponse
import com.borrowingservice.model.valueObject.dto.BorrowingBanResponse
import com.borrowingservice.model.valueObject.enums.BanTier
import com.borrowingservice.model.view.BanPeriodView
import com.borrowingservice.model.view.BorrowingBanRecordView
import com.borrowingservice.repository.BorrowingBanRecordViewRepository
import com.borrowingservice.service.BorrowingBanViewReadService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.ZonedDateTime

@Service
@Transactional(readOnly = true)
class BorrowingBanViewReadServiceImpl(
    private val banRecordViewRepository: BorrowingBanRecordViewRepository,
    private val clock: Clock
) : BorrowingBanViewReadService {
    override fun findById(banRecordId: String): BorrowingBanResponse? =
        banRecordViewRepository.findById(banRecordId).orElse(null)?.toResponse()

    override fun findAll(): List<BorrowingBanResponse> =
        banRecordViewRepository.findAll().map { it.toResponse() }

    override fun findByMemberId(memberId: String): BorrowingBanResponse? =
        banRecordViewRepository.findByMemberId(memberId)?.toResponse()

    private fun BorrowingBanRecordView.toResponse(): BorrowingBanResponse {
        val now = ZonedDateTime.now(clock)
        val history = bans.sortedBy { it.issuedAt }.map { it.toResponse() }
        val current = history
            .filter { !it.startsAt.isAfter(now) && (it.endsAt == null || it.endsAt.isAfter(now)) }
            .maxByOrNull { it.startsAt }

        return BorrowingBanResponse(
            banRecordId = banRecordId,
            memberId = memberId,
            lastIssuedTier = lastIssuedTier,
            active = current != null,
            permanentlyBanned = current?.tier == BanTier.PERMANENT,
            currentBan = current,
            history = history
        )
    }

    private fun BanPeriodView.toResponse() = BanPeriodResponse(
        banId = banId,
        tier = tier,
        startsAt = startsAt,
        endsAt = endsAt,
        issuedAt = issuedAt,
        triggeringFeeId = triggeringFeeId,
        reason = reason
    )
}
