package com.borrowingservice.model.view

import com.borrowingservice.model.valueObject.enums.BanReason
import com.borrowingservice.model.valueObject.enums.BanTier
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "ban_period_view")
class BanPeriodView(
    @Id
    @Column(name = "ban_id", nullable = false, updatable = false)
    var banId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var tier: BanTier,

    @Column(name = "starts_at", nullable = false)
    var startsAt: ZonedDateTime,

    @Column(name = "ends_at")
    var endsAt: ZonedDateTime?,

    @Column(name = "issued_at", nullable = false)
    var issuedAt: ZonedDateTime,

    @Column(name = "triggering_fee_id", nullable = false, updatable = false)
    var triggeringFeeId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var reason: BanReason
)
