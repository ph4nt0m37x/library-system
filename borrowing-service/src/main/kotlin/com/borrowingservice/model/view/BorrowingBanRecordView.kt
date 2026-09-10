package com.borrowingservice.model.view

import com.borrowingservice.model.valueObject.enums.BanTier
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "borrowing_ban_record_view")
class BorrowingBanRecordView(
    @Id
    @Column(name = "ban_record_id", nullable = false, updatable = false)
    var banRecordId: String,

    @Column(name = "member_id", nullable = false, unique = true, updatable = false)
    var memberId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "last_issued_tier", nullable = false)
    var lastIssuedTier: BanTier,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ban_record_id",
        referencedColumnName = "ban_record_id",
        nullable = false
    )
    var bans: MutableList<BanPeriodView> = mutableListOf()
)
