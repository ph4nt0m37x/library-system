package com.membershipservice.model.view

import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.PhoneNumber
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "member_view")
class MemberView(
    @EmbeddedId
    @AttributeOverride(name = "value", column = Column(name = "member_id", nullable = false, updatable = false))
    var memberId: MemberId,

    @Embedded
    @AttributeOverride(
        name = "value",
        column = Column(name = "membership_number", nullable = false, unique = true, length = 64)
    )
    var membershipNumber: MembershipNumber,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Embedded
    @AttributeOverride(
        name = "value",
        column = Column(name = "email", nullable = false, unique = true, length = 254)
    )
    var email: Email,

    @Embedded
    @AttributeOverride(name = "value", column = Column(name = "phone_number", nullable = false, length = 20))
    var phoneNumber: PhoneNumber,

    @Column(name = "registered_at", nullable = false)
    var registeredAt: ZonedDateTime,

    @Column(name = "changed_at")
    var changedAt: ZonedDateTime? = null
)
