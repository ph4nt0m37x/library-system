package com.borrowingservice.model.aggregate

import com.borrowingservice.handlers.eventSourcingHandlers.BorrowingBanRecordEventSourcingHandler
import com.borrowingservice.model.entity.BanPeriod
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
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateMember
import org.axonframework.spring.stereotype.Aggregate
import java.time.ZonedDateTime

@Aggregate(repository = "axonBorrowingBanRecordRepository")
@Entity
@Table(name = "borrowing_ban_records")
class BorrowingBanRecord : BorrowingBanRecordEventSourcingHandler() {
    @AggregateIdentifier
    @Id
    @Column(name = "ban_record_id", nullable = false, updatable = false)
    override lateinit var banRecordId: String
        protected set

    @Column(name = "member_id", nullable = false, unique = true, updatable = false)
    override lateinit var memberId: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "last_issued_tier", nullable = false)
    override lateinit var lastIssuedTier: BanTier
        protected set

    @AggregateMember
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(
        name = "ban_record_id",
        referencedColumnName = "ban_record_id",
        nullable = false
    )
    override val bans: MutableList<BanPeriod> = mutableListOf()

    fun hasTier(tier: BanTier): Boolean = bans.any { it.tier == tier }

    fun hasPermanentBan(): Boolean = bans.any { it.tier == BanTier.PERMANENT }

    fun hasActiveTemporaryBan(at: ZonedDateTime): Boolean = bans.any {
        it.tier != BanTier.PERMANENT &&
            !it.startsAt.isAfter(at) &&
            it.endsAt?.isAfter(at) == true
    }
}
