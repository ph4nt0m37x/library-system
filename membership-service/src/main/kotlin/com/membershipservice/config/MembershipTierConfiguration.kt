package com.membershipservice.config

import com.membershipservice.model.valueObject.enums.Tier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Currency

data class MembershipTierDefinition(
    val tierCode: Tier,
    val durationMonths: Int,
    val price: BigDecimal,
    val currency: String
)

@Component
class MembershipTierConfiguration(
    @Value("\${membership.tiers.three-months.duration-months:3}") threeMonths: Int,
    @Value("\${membership.tiers.six-months.duration-months:6}") sixMonths: Int,
    @Value("\${membership.tiers.twelve-months.duration-months:12}") twelveMonths: Int,
    @Value("\${membership.tiers.three-months.price:900.00}") threeMonthsPrice: BigDecimal,
    @Value("\${membership.tiers.six-months.price:1500.00}") sixMonthsPrice: BigDecimal,
    @Value("\${membership.tiers.twelve-months.price:2400.00}") twelveMonthsPrice: BigDecimal,
    @Value("\${membership.payments.currency:MKD}") currency: String
) {
    private val configuredCurrency = currency.trim().uppercase()

    private val definitions = listOf(
        MembershipTierDefinition(Tier.THREE_MONTHS, threeMonths, threeMonthsPrice, configuredCurrency),
        MembershipTierDefinition(Tier.SIX_MONTHS, sixMonths, sixMonthsPrice, configuredCurrency),
        MembershipTierDefinition(Tier.TWELVE_MONTHS, twelveMonths, twelveMonthsPrice, configuredCurrency)
    ).associateBy { it.tierCode }

    init {
        require(definitions.values.all { it.durationMonths > 0 }) { "Membership tier durations must be positive" }
        require(definitions.values.all { it.price > BigDecimal.ZERO }) { "Membership tier prices must be positive" }
        require(Currency.getInstance(configuredCurrency).currencyCode == configuredCurrency) {
            "Membership payment currency must be a valid ISO 4217 currency"
        }
    }

    fun definitionFor(tier: Tier): MembershipTierDefinition =
        definitions.getValue(tier)
}
