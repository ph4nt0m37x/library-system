package com.membershipservice.config

import com.membershipservice.model.valueObject.enums.Tier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

data class MembershipTierDefinition(
    val tierCode: Tier,
    val durationMonths: Int
)

@Component
class MembershipTierConfiguration(
    @Value("\${membership.tiers.three-months.duration-months:3}") threeMonths: Int,
    @Value("\${membership.tiers.six-months.duration-months:6}") sixMonths: Int,
    @Value("\${membership.tiers.twelve-months.duration-months:12}") twelveMonths: Int
) {
    private val definitions = listOf(
        MembershipTierDefinition(Tier.THREE_MONTHS, threeMonths),
        MembershipTierDefinition(Tier.SIX_MONTHS, sixMonths),
        MembershipTierDefinition(Tier.TWELVE_MONTHS, twelveMonths)
    ).associateBy { it.tierCode }

    init {
        require(definitions.values.all { it.durationMonths > 0 }) { "Membership tier durations must be positive" }
    }

    fun definitionFor(tier: Tier): MembershipTierDefinition =
        definitions.getValue(tier)
}
