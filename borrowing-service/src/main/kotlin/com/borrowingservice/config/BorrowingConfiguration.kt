package com.borrowingservice.config

import com.borrowingservice.model.valueObject.enums.BanTier
import com.borrowingservice.model.valueObject.enums.BillableDayRule
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Period
import java.time.ZoneId

@Component
class LoanPolicyConfiguration(
    @Value("\${borrowing.limits.max-active-loans}") val maxActiveLoans: Long,
    @Value("\${borrowing.limits.max-unpaid-fees}") val maxUnpaidFees: Long,
    @Value("\${borrowing.periods.regular-loan}") regularLoanDuration: String,
    @Value("\${borrowing.periods.extension}") extensionDuration: String
) {
    val regularLoanDuration: Duration = Duration.parse(regularLoanDuration)
    val extensionDuration: Duration = Duration.parse(extensionDuration)

    init {
        require(maxActiveLoans > 0) { "The active Loan limit must be positive" }
        require(maxUnpaidFees > 0) { "The unpaid Fee limit must be positive" }
        require(!this.regularLoanDuration.isNegative && !this.regularLoanDuration.isZero) {
            "The regular Loan duration must be positive"
        }
        require(!this.extensionDuration.isNegative && !this.extensionDuration.isZero) {
            "The Loan extension duration must be positive"
        }
    }
}

@Component
class FeePolicyConfiguration(
    @Value("\${borrowing.fees.currency}") currency: String,
    @Value("\${borrowing.fees.daily-late-rate}") val dailyLateRate: BigDecimal,
    @Value("\${borrowing.fees.escalation-threshold-days}") val escalationThresholdDays: Long,
    @Value("\${borrowing.fees.replacement-multiplier}") val replacementMultiplier: BigDecimal,
    @Value("\${borrowing.fees.money-scale}") val moneyScale: Int,
    @Value("\${borrowing.fees.rounding-mode}") roundingMode: String,
    @Value("\${borrowing.fees.billable-time-zone:}") billableTimeZone: String,
    @Value("\${borrowing.fees.billable-day-rule:}") billableDayRule: String
) {
    val currency: String = currency.trim().uppercase()
    val roundingMode: RoundingMode = RoundingMode.valueOf(roundingMode.trim().uppercase())
    val billableTimeZone: ZoneId = billableTimeZone.trim()
        .also { require(it.isNotBlank()) { "borrowing.fees.billable-time-zone must be configured explicitly" } }
        .let(ZoneId::of)
    val billableDayRule: BillableDayRule = billableDayRule.trim()
        .also { require(it.isNotBlank()) { "borrowing.fees.billable-day-rule must be configured explicitly" } }
        .let { BillableDayRule.valueOf(it.uppercase()) }

    init {
        require(CURRENCY_PATTERN.matches(this.currency)) { "Fee currency must be a three-letter ISO 4217 code" }
        require(dailyLateRate >= BigDecimal.ZERO) { "Daily late rate must not be negative" }
        require(escalationThresholdDays > 0) { "Fee escalation threshold must be positive" }
        require(replacementMultiplier > BigDecimal.ZERO) { "Replacement multiplier must be positive" }
        require(moneyScale >= 0) { "Money scale must not be negative" }
    }

    private companion object {
        val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}

data class BanTierDefinition(
    val tier: BanTier,
    val damageThreshold: Long,
    val duration: Period?
)

@Component
class BanPolicyConfiguration(
    @Value("\${borrowing.bans.tier-1.damage-threshold}") tierOneThreshold: Long,
    @Value("\${borrowing.bans.tier-1.duration}") tierOneDuration: String,
    @Value("\${borrowing.bans.tier-2.damage-threshold}") tierTwoThreshold: Long,
    @Value("\${borrowing.bans.tier-2.duration}") tierTwoDuration: String,
    @Value("\${borrowing.bans.permanent.damage-threshold}") permanentThreshold: Long
) {
    private val definitions = linkedMapOf(
        BanTier.TIER_1 to BanTierDefinition(BanTier.TIER_1, tierOneThreshold, Period.parse(tierOneDuration)),
        BanTier.TIER_2 to BanTierDefinition(BanTier.TIER_2, tierTwoThreshold, Period.parse(tierTwoDuration)),
        BanTier.PERMANENT to BanTierDefinition(BanTier.PERMANENT, permanentThreshold, null)
    )

    init {
        val thresholds = definitions.values.map { it.damageThreshold }
        require(thresholds.all { it > 0 }) { "Damage thresholds must be positive" }
        require(thresholds.zipWithNext().all { (lower, upper) -> lower < upper }) {
            "Damage thresholds must be strictly increasing"
        }
        require(definitions.values.mapNotNull { it.duration }.all { !it.isZero && !it.isNegative }) {
            "Temporary ban durations must be positive"
        }
    }

    fun definitionFor(tier: BanTier): BanTierDefinition = definitions.getValue(tier)

    fun nextTierAfter(tier: BanTier): BanTier? = when (tier) {
        BanTier.NONE -> BanTier.TIER_1
        BanTier.TIER_1 -> BanTier.TIER_2
        BanTier.TIER_2 -> BanTier.PERMANENT
        BanTier.PERMANENT -> null
    }
}
