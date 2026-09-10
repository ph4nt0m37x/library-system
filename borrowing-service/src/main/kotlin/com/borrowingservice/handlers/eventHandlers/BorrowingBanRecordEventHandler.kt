package com.borrowingservice.handlers.eventHandlers

import com.borrowingservice.model.event.BorrowingBanIssuedEvent
import com.borrowingservice.model.view.BanPeriodView
import com.borrowingservice.model.view.BorrowingBanRecordView
import com.borrowingservice.repository.BorrowingBanRecordViewRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ProcessingGroup("borrowing-projections")
class BorrowingBanRecordEventHandler(
    private val banRecordViewRepository: BorrowingBanRecordViewRepository
) {
    @EventHandler
    @Transactional
    fun on(event: BorrowingBanIssuedEvent) {
        val view = banRecordViewRepository.findById(event.banRecordId).orElse(null)
            ?: BorrowingBanRecordView(
                banRecordId = event.banRecordId,
                memberId = event.memberId,
                lastIssuedTier = event.tier
            )

        if (view.bans.any { it.banId == event.banId }) {
            return
        }

        view.lastIssuedTier = event.tier
        view.bans.add(
            BanPeriodView(
                banId = event.banId,
                tier = event.tier,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                issuedAt = event.issuedAt,
                triggeringFeeId = event.triggeringFeeId,
                reason = event.reason
            )
        )
        banRecordViewRepository.save(view)
    }
}
