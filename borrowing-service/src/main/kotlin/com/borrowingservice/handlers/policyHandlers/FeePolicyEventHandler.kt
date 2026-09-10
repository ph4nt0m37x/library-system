package com.borrowingservice.handlers.policyHandlers

import com.borrowingservice.config.BanPolicyConfiguration
import com.borrowingservice.model.command.IssueBorrowingBanCommand
import com.borrowingservice.model.event.DamagedBookFeeCreatedEvent
import com.borrowingservice.model.valueObject.enums.BanReason
import com.borrowingservice.model.valueObject.enums.BanTier
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.repository.BorrowingBanRecordRepository
import com.borrowingservice.repository.FeeRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.ZonedDateTime
import java.util.UUID

@Component
@ProcessingGroup("borrowing-policies")
class FeePolicyEventHandler(
    private val commandGateway: CommandGateway,
    private val feeRepository: FeeRepository,
    private val banRecordRepository: BorrowingBanRecordRepository,
    private val banPolicy: BanPolicyConfiguration,
    private val clock: Clock
) {
    @EventHandler
    @Transactional
    fun on(event: DamagedBookFeeCreatedEvent) {
        val persistedDamageCount = feeRepository.countByLoanMemberIdAndReason(event.memberId, FeeReason.DAMAGED)
        val damageCount = persistedDamageCount + if (feeRepository.existsById(event.feeId)) 0 else 1
        val record = banRecordRepository.findByMemberId(event.memberId)
        val lastTier = record?.lastIssuedTier ?: BanTier.NONE
        val nextTier = banPolicy.nextTierAfter(lastTier) ?: return
        val nextDefinition = banPolicy.definitionFor(nextTier)
        if (damageCount < nextDefinition.damageThreshold) {
            return
        }

        val issuedAt = now()
        if (record?.hasActiveTemporaryBan(issuedAt) == true) {
            log.warn(
                "Damage threshold for {} was reached by member {} while a temporary ban is active; " +
                    "the next tier is intentionally not issued until this open business decision is resolved",
                nextTier,
                event.memberId
            )
            return
        }

        commandGateway.sendAndWait<String>(
            IssueBorrowingBanCommand(
                banRecordId = banRecordIdFor(event.memberId),
                banId = banIdFor(event.memberId, nextTier),
                memberId = event.memberId,
                tier = nextTier,
                startsAt = issuedAt,
                endsAt = nextDefinition.duration?.let { issuedAt.plus(it) },
                issuedAt = issuedAt,
                triggeringFeeId = event.feeId,
                reason = BanReason.REPEATED_PERMANENT_BOOK_DAMAGE
            )
        )
    }

    private fun banRecordIdFor(memberId: String): String = deterministicId("ban-record:member:$memberId")

    private fun banIdFor(memberId: String, tier: BanTier): String =
        deterministicId("ban:member:$memberId:tier:${tier.name}")

    private fun deterministicId(source: String): String =
        UUID.nameUUIDFromBytes(source.toByteArray(StandardCharsets.UTF_8)).toString()

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)

    private companion object {
        val log = LoggerFactory.getLogger(FeePolicyEventHandler::class.java)
    }
}
