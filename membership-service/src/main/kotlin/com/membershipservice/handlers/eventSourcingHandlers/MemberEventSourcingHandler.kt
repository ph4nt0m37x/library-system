package com.membershipservice.handlers.eventSourcingHandlers

import com.membershipservice.model.entity.SubscriptionPeriod
import com.membershipservice.model.event.MemberContactDetailsUpdatedEvent
import com.membershipservice.model.event.MemberNameUpdatedEvent
import com.membershipservice.model.event.MemberRegisteredEvent
import com.membershipservice.model.event.SubscriptionRenewedEvent
import com.membershipservice.model.event.SubscriptionStartedEvent
import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.PhoneNumber
import org.axonframework.eventsourcing.EventSourcingHandler
import java.time.ZonedDateTime

abstract class MemberEventSourcingHandler {
    abstract var memberId: MemberId
        protected set

    abstract var membershipNumber: MembershipNumber
        protected set

    abstract var firstName: String
        protected set

    abstract var lastName: String
        protected set

    abstract var email: Email
        protected set

    abstract var phoneNumber: PhoneNumber
        protected set

    abstract var registeredAt: ZonedDateTime
        protected set

    abstract val subscriptions: MutableList<SubscriptionPeriod>

    @EventSourcingHandler
    fun on(event: MemberRegisteredEvent) {
        memberId = event.memberId
        membershipNumber = event.membershipNumber
        firstName = event.firstName
        lastName = event.lastName
        email = event.email
        phoneNumber = event.phoneNumber
        registeredAt = event.registeredAt
    }

    @EventSourcingHandler
    fun on(event: MemberNameUpdatedEvent) {
        firstName = event.firstName
        lastName = event.lastName
    }

    @EventSourcingHandler
    fun on(event: MemberContactDetailsUpdatedEvent) {
        email = event.email
        phoneNumber = event.phoneNumber
    }

    @EventSourcingHandler
    fun on(event: SubscriptionStartedEvent) {
        subscriptions.add(event.toSubscriptionPeriod())
    }

    @EventSourcingHandler
    fun on(event: SubscriptionRenewedEvent) {
        subscriptions.add(event.toSubscriptionPeriod())
    }

    private fun SubscriptionStartedEvent.toSubscriptionPeriod() = SubscriptionPeriod(
        subscriptionId, memberId, tier, startsAt, endsAt, createdAt, amountPaid, currency, paidAt,
        paymentReference, paymentStatus
    )

    private fun SubscriptionRenewedEvent.toSubscriptionPeriod() = SubscriptionPeriod(
        subscriptionId, memberId, tier, startsAt, endsAt, createdAt, amountPaid, currency, paidAt,
        paymentReference, paymentStatus
    )
}
