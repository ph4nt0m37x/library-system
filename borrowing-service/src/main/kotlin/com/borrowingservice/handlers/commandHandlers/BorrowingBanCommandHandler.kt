package com.borrowingservice.handlers.commandHandlers

import com.borrowingservice.model.aggregate.BorrowingBanRecord
import com.borrowingservice.model.command.IssueBorrowingBanCommand
import com.borrowingservice.model.event.BorrowingBanIssuedEvent
import com.borrowingservice.model.valueObject.enums.BanTier
import com.borrowingservice.repository.BorrowingBanRecordRepository
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BorrowingBanCommandHandler(
    @Qualifier("axonBorrowingBanRecordRepository")
    private val banAggregateRepository: Repository<BorrowingBanRecord>,
    private val banRecordRepository: BorrowingBanRecordRepository
) {
    @CommandHandler
    @Transactional
    fun handle(command: IssueBorrowingBanCommand): String {
        require(command.tier != BanTier.NONE) { "NONE is not an issuable ban tier" }
        require((command.tier == BanTier.PERMANENT) == (command.endsAt == null)) {
            "Only a PERMANENT ban may have no endsAt"
        }
        command.endsAt?.let { require(it.isAfter(command.startsAt)) { "A temporary ban must end after it starts" } }

        val existing = banRecordRepository.findByMemberId(command.memberId)
        if (existing == null) {
            require(command.tier == BanTier.TIER_1) { "The first issued ban must be TIER_1" }
            applyNewRecord(command)
            return command.banId
        }

        require(existing.banRecordId == command.banRecordId) {
            "Member ${command.memberId} already uses ban record ${existing.banRecordId}"
        }
        existing.bans.firstOrNull { it.tier == command.tier }?.let { issued ->
            require(issued.banId == command.banId && issued.triggeringFeeId == command.triggeringFeeId) {
                "Ban tier ${command.tier} has already been issued"
            }
            return issued.banId
        }
        require(command.tier == nextTier(existing.lastIssuedTier)) {
            "Ban tiers must progress in order after ${existing.lastIssuedTier}"
        }

        banAggregateRepository.load(existing.banRecordId).execute { record ->
            if (!record.hasTier(command.tier)) {
                AggregateLifecycle.apply(command.toEvent())
            }
        }
        return command.banId
    }

    private fun applyNewRecord(command: IssueBorrowingBanCommand) {
        banAggregateRepository.newInstance {
            BorrowingBanRecord().also { AggregateLifecycle.apply(command.toEvent()) }
        }
    }

    private fun IssueBorrowingBanCommand.toEvent() = BorrowingBanIssuedEvent(
        banRecordId,
        banId,
        memberId,
        tier,
        startsAt,
        endsAt,
        issuedAt,
        triggeringFeeId,
        reason
    )

    private fun nextTier(tier: BanTier): BanTier? = when (tier) {
        BanTier.NONE -> BanTier.TIER_1
        BanTier.TIER_1 -> BanTier.TIER_2
        BanTier.TIER_2 -> BanTier.PERMANENT
        BanTier.PERMANENT -> null
    }
}
