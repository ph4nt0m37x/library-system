package com.membershipservice.handlers.eventHandlers

import com.membershipservice.model.event.MemberContactDetailsUpdatedEvent
import com.membershipservice.model.event.MemberNameUpdatedEvent
import com.membershipservice.model.event.MemberRegisteredEvent
import com.membershipservice.model.event.SubscriptionRenewedEvent
import com.membershipservice.model.event.SubscriptionStartedEvent
import com.membershipservice.model.view.MemberView
import com.membershipservice.model.view.SubscriptionPeriodView
import com.membershipservice.repository.MemberViewRepository
import com.membershipservice.repository.SubscriptionPeriodViewRepository
import org.axonframework.eventhandling.EventHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberEventHandler(
    private val memberViewRepository: MemberViewRepository,
    private val subscriptionPeriodViewRepository: SubscriptionPeriodViewRepository
) {
    @EventHandler
    @Transactional
    fun on(event: MemberRegisteredEvent) {
        memberViewRepository.save(
            MemberView(
                memberId = event.memberId,
                membershipNumber = event.membershipNumber,
                firstName = event.firstName,
                lastName = event.lastName,
                email = event.email,
                phoneNumber = event.phoneNumber,
                registeredAt = event.registeredAt
            )
        )
    }

    @EventHandler
    @Transactional
    fun on(event: MemberNameUpdatedEvent) {
        val view = memberViewRepository.findById(event.memberId)
            .orElseThrow { IllegalStateException("Member projection not found: ${event.memberId}") }
        view.firstName = event.firstName
        view.lastName = event.lastName
        view.changedAt = event.changedAt
        memberViewRepository.save(view)
    }

    @EventHandler
    @Transactional
    fun on(event: MemberContactDetailsUpdatedEvent) {
        val view = memberViewRepository.findById(event.memberId)
            .orElseThrow { IllegalStateException("Member projection not found: ${event.memberId}") }
        view.email = event.email
        view.phoneNumber = event.phoneNumber
        view.changedAt = event.changedAt
        memberViewRepository.save(view)
    }

    @EventHandler
    @Transactional
    fun on(event: SubscriptionStartedEvent) {
        if (!subscriptionPeriodViewRepository.existsById(event.subscriptionId)) {
            subscriptionPeriodViewRepository.save(
                SubscriptionPeriodView(
                    subscriptionId = event.subscriptionId,
                    memberId = event.memberId,
                    previousSubscriptionId = null,
                    tier = event.tier,
                    startsAt = event.startsAt,
                    endsAt = event.endsAt,
                    createdAt = event.createdAt,
                    amountPaid = event.amountPaid,
                    currency = event.currency,
                    paidAt = event.paidAt,
                    paymentReference = event.paymentReference,
                    paymentStatus = event.paymentStatus
                )
            )
        }
    }

    @EventHandler
    @Transactional
    fun on(event: SubscriptionRenewedEvent) {
        if (!subscriptionPeriodViewRepository.existsById(event.subscriptionId)) {
            subscriptionPeriodViewRepository.save(
                SubscriptionPeriodView(
                    subscriptionId = event.subscriptionId,
                    memberId = event.memberId,
                    previousSubscriptionId = event.previousSubscriptionId,
                    tier = event.tier,
                    startsAt = event.startsAt,
                    endsAt = event.endsAt,
                    createdAt = event.createdAt,
                    amountPaid = event.amountPaid,
                    currency = event.currency,
                    paidAt = event.paidAt,
                    paymentReference = event.paymentReference,
                    paymentStatus = event.paymentStatus
                )
            )
        }
    }
}
