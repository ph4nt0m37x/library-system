package com.borrowingservice.service

import com.borrowingservice.config.BanPolicyConfiguration
import com.borrowingservice.model.command.IssueBorrowingBanCommand
import com.borrowingservice.model.valueObject.enums.BanReason
import com.borrowingservice.model.valueObject.enums.BanTier
import com.borrowingservice.model.valueObject.enums.FeeReason
import com.borrowingservice.repository.BorrowingBanRecordRepository
import com.borrowingservice.repository.FeeRepository
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.ZonedDateTime
import java.util.UUID

@Component
class BorrowingBanEscalationScheduler(
    private val banRecordRepository: BorrowingBanRecordRepository,
    private val feeRepository: FeeRepository,
    private val banPolicy: BanPolicyConfiguration,
    private val commandGateway: CommandGateway,
    private val clock: Clock
) {
    @Scheduled(fixedDelayString = "\${borrowing.bans.escalation-check-delay-ms:60000}")
    fun issueDeferredEscalations() {
        val now = ZonedDateTime.now(clock)
        banRecordRepository.findAll().forEach { record ->
            if (record.lastIssuedTier == BanTier.PERMANENT || record.hasActiveTemporaryBan(now)) return@forEach

            val nextTier = banPolicy.nextTierAfter(record.lastIssuedTier) ?: return@forEach
            val definition = banPolicy.definitionFor(nextTier)
            val damageCount = feeRepository.countByLoanMemberIdAndReason(record.memberId, FeeReason.DAMAGED)
            if (damageCount < definition.damageThreshold) return@forEach

            val triggeringFee = feeRepository.findFirstByMemberIdAndReasonOrderByCreatedAtDesc(
                record.memberId,
                FeeReason.DAMAGED
            ) ?: return@forEach

            runCatching {
                commandGateway.sendAndWait<String>(
                    IssueBorrowingBanCommand(
                        banRecordId = record.banRecordId,
                        banId = deterministicId("ban:member:${record.memberId}:tier:${nextTier.name}"),
                        memberId = record.memberId,
                        tier = nextTier,
                        startsAt = now,
                        endsAt = definition.duration?.let(now::plus),
                        issuedAt = now,
                        triggeringFeeId = triggeringFee.feeId,
                        reason = BanReason.REPEATED_PERMANENT_BOOK_DAMAGE
                    )
                )
            }.onFailure {
                log.warn("Could not issue deferred {} ban for member {}", nextTier, record.memberId, it)
            }
        }
    }

    private fun deterministicId(value: String): String =
        UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8)).toString()

    private companion object {
        val log = LoggerFactory.getLogger(BorrowingBanEscalationScheduler::class.java)
    }
}
