package com.membershipservice.service

import com.membershipservice.model.command.RegisterMemberCommand
import com.membershipservice.model.command.RenewSubscriptionCommand
import com.membershipservice.model.command.StartSubscriptionCommand
import com.membershipservice.model.command.UpdateMemberContactDetailsCommand
import com.membershipservice.model.command.UpdateMemberNameCommand
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.SubscriptionId
import java.util.concurrent.CompletableFuture

interface MemberService {
    fun registerMember(command: RegisterMemberCommand): CompletableFuture<MemberId>
    fun updateMemberName(command: UpdateMemberNameCommand): CompletableFuture<MemberId>
    fun updateMemberContactDetails(command: UpdateMemberContactDetailsCommand): CompletableFuture<MemberId>
    fun startSubscription(command: StartSubscriptionCommand): CompletableFuture<SubscriptionId>
    fun renewSubscription(command: RenewSubscriptionCommand): CompletableFuture<SubscriptionId>
}
