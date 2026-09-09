package com.membershipservice.model.aggregate

import com.membershipservice.handlers.eventSourcingHandlers.MemberEventSourcingHandler
import com.membershipservice.model.entity.SubscriptionPeriod
import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.PhoneNumber
import jakarta.persistence.AttributeOverride
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateMember
import org.axonframework.spring.stereotype.Aggregate
import java.time.ZonedDateTime

@Aggregate(repository = "axonMemberRepository")
@Entity
@Table(name = "members")
class Member : MemberEventSourcingHandler() {
    @AggregateIdentifier
    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "member_id", nullable = false, updatable = false))
    override lateinit var memberId: MemberId
        protected set

    @Embedded
    @AttributeOverride(
        name = "value",
        column = Column(name = "membership_number", nullable = false, unique = true, length = 64)
    )
    override lateinit var membershipNumber: MembershipNumber
        protected set

    @Column(name = "first_name", nullable = false, length = 100)
    override lateinit var firstName: String
        protected set

    @Column(name = "last_name", nullable = false, length = 100)
    override lateinit var lastName: String
        protected set

    @Embedded
    @AttributeOverride(
        name = "value",
        column = Column(name = "email", nullable = false, unique = true, length = 254)
    )
    override lateinit var email: Email
        protected set

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "phone_number", nullable = false, length = 20))
    override lateinit var phoneNumber: PhoneNumber
        protected set

    @Column(name = "registered_at", nullable = false)
    override lateinit var registeredAt: ZonedDateTime
        protected set

    @AggregateMember
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "member_id",
        referencedColumnName = "member_id",
        insertable = false,
        updatable = false
    )
    override val subscriptions: MutableList<SubscriptionPeriod> = mutableListOf()
}
