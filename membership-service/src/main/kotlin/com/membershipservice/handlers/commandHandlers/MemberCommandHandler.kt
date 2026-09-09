package com.membershipservice.handlers.commandHandlers

import com.membershipservice.config.MembershipTierConfiguration
import com.membershipservice.model.aggregate.Member
import com.membershipservice.model.command.RegisterMemberCommand
import com.membershipservice.model.command.RenewSubscriptionCommand
import com.membershipservice.model.command.StartSubscriptionCommand
import com.membershipservice.model.command.UpdateMemberContactDetailsCommand
import com.membershipservice.model.command.UpdateMemberNameCommand
import com.membershipservice.model.event.MemberContactDetailsUpdatedEvent
import com.membershipservice.model.event.MemberNameUpdatedEvent
import com.membershipservice.model.event.MemberRegisteredEvent
import com.membershipservice.model.event.SubscriptionRenewedEvent
import com.membershipservice.model.event.SubscriptionStartedEvent
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.SubscriptionId
import com.membershipservice.repository.MemberRepository
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.Repository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.ZonedDateTime

@Component
class MemberCommandHandler(
    @Qualifier("axonMemberRepository") private val memberAggregateRepository: Repository<Member>,
    private val memberRepository: MemberRepository,
    private val tierConfiguration: MembershipTierConfiguration,
    private val subscriptionDateCalculator: SubscriptionDateCalculator,
    private val clock: Clock
) {
    @CommandHandler
    fun handle(command: RegisterMemberCommand): MemberId {
        validateName(command.firstName, command.lastName)
        val memberId = MemberId()
        val membershipNumber = MembershipNumber(memberId.baseValue())

        require(!memberRepository.existsById(memberId)) { "Generated member ID already exists" }
        require(!memberRepository.existsByMembershipNumber(membershipNumber)) {
            "Membership number already exists"
        }
        require(memberRepository.findByEmail(command.email) == null) { "Email address already exists" }

        val event = MemberRegisteredEvent(
            memberId = memberId,
            membershipNumber = membershipNumber,
            firstName = command.firstName.trim(),
            lastName = command.lastName.trim(),
            email = command.email,
            phoneNumber = command.phoneNumber,
            registeredAt = ZonedDateTime.now(clock)
        )
        memberAggregateRepository.newInstance {
            Member().also { AggregateLifecycle.apply(event) }
        }
        return memberId
    }

    @CommandHandler
    fun handle(command: UpdateMemberNameCommand): MemberId {
        validateName(command.firstName, command.lastName)
        val changedAt = ZonedDateTime.now(clock)
        memberAggregateRepository.load(command.memberId.prefixedValue()).execute { member ->
            val firstName = command.firstName.trim()
            val lastName = command.lastName.trim()
            require(firstName != member.firstName || lastName != member.lastName) {
                "The supplied name is already current"
            }
            AggregateLifecycle.apply(MemberNameUpdatedEvent(command.memberId, firstName, lastName, changedAt))
        }
        return command.memberId
    }

    @CommandHandler
    fun handle(command: UpdateMemberContactDetailsCommand): MemberId {
        val changedAt = ZonedDateTime.now(clock)
        memberRepository.findByEmail(command.email)?.let {
            require(it.memberId == command.memberId) { "Email address already exists" }
        }
        memberAggregateRepository.load(command.memberId.prefixedValue()).execute { member ->
            require(command.email != member.email || command.phoneNumber != member.phoneNumber) {
                "The supplied contact details are already current"
            }
            AggregateLifecycle.apply(
                MemberContactDetailsUpdatedEvent(
                    command.memberId,
                    command.email,
                    command.phoneNumber,
                    changedAt
                )
            )
        }
        return command.memberId
    }

    @CommandHandler
    fun handle(command: StartSubscriptionCommand): SubscriptionId {
        val durationMonths = tierConfiguration.definitionFor(command.tier).durationMonths
        validatePayment(command.amountPaid, command.currency)
        val subscriptionId = SubscriptionId()
        val createdAt = ZonedDateTime.now(clock)

        memberAggregateRepository.load(command.memberId.prefixedValue()).execute { member ->
            require(member.subscriptions.isEmpty()) { "A subscription has already been started for this member" }
            AggregateLifecycle.apply(
                SubscriptionStartedEvent(
                    subscriptionId = subscriptionId,
                    memberId = command.memberId,
                    tier = command.tier,
                    startsAt = command.startsAt,
                    endsAt = subscriptionDateCalculator.endsAt(command.startsAt, durationMonths),
                    createdAt = createdAt,
                    amountPaid = command.amountPaid,
                    currency = command.currency.trim().uppercase(),
                    paidAt = command.paidAt
                )
            )
        }
        return subscriptionId
    }

    @CommandHandler
    fun handle(command: RenewSubscriptionCommand): SubscriptionId {
        val durationMonths = tierConfiguration.definitionFor(command.tier).durationMonths
        validatePayment(command.amountPaid, command.currency)
        val subscriptionId = SubscriptionId()
        val createdAt = ZonedDateTime.now(clock)

        memberAggregateRepository.load(command.memberId.prefixedValue()).execute { member ->
            require(member.subscriptions.isNotEmpty()) {
                "A subscription must be started before it can be renewed"
            }
            val previousPeriod = member.subscriptions.maxBy { it.endsAt }
            val startsAt = subscriptionDateCalculator.renewalStartsAt(createdAt, previousPeriod.endsAt)
            AggregateLifecycle.apply(
                SubscriptionRenewedEvent(
                    subscriptionId = subscriptionId,
                    memberId = command.memberId,
                    previousSubscriptionId = previousPeriod.subscriptionId,
                    tier = command.tier,
                    startsAt = startsAt,
                    endsAt = subscriptionDateCalculator.endsAt(startsAt, durationMonths),
                    createdAt = createdAt,
                    amountPaid = command.amountPaid,
                    currency = command.currency.trim().uppercase(),
                    paidAt = command.paidAt
                )
            )
        }
        return subscriptionId
    }

    private fun validateName(firstName: String, lastName: String) {
        require(firstName.isNotBlank()) { "First name must not be blank" }
        require(lastName.isNotBlank()) { "Last name must not be blank" }
        require(firstName.trim().length <= 100) { "First name must not exceed 100 characters" }
        require(lastName.trim().length <= 100) { "Last name must not exceed 100 characters" }
    }

    private fun validatePayment(amountPaid: BigDecimal, currency: String) {
        require(amountPaid >= BigDecimal.ZERO) { "Amount paid must not be negative" }
        require(CURRENCY_PATTERN.matches(currency.trim().uppercase())) {
            "Currency must be a three-letter ISO 4217 code"
        }
    }

    private companion object {
        val CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}
