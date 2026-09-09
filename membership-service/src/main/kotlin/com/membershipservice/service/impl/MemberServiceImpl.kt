package com.membershipservice.service.impl

import com.membershipservice.model.command.RegisterMemberCommand
import com.membershipservice.model.command.RenewSubscriptionCommand
import com.membershipservice.model.command.StartSubscriptionCommand
import com.membershipservice.model.command.UpdateMemberContactDetailsCommand
import com.membershipservice.model.command.UpdateMemberNameCommand
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.SubscriptionId
import com.membershipservice.service.MemberService
import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
class MemberServiceImpl(
    private val commandGateway: CommandGateway
) : MemberService {
    override fun registerMember(command: RegisterMemberCommand): CompletableFuture<MemberId> =
        commandGateway.send(command)

    override fun updateMemberName(command: UpdateMemberNameCommand): CompletableFuture<MemberId> =
        commandGateway.send(command)

    override fun updateMemberContactDetails(
        command: UpdateMemberContactDetailsCommand
    ): CompletableFuture<MemberId> = commandGateway.send(command)

    override fun startSubscription(command: StartSubscriptionCommand): CompletableFuture<SubscriptionId> =
        commandGateway.send(command)

    override fun renewSubscription(command: RenewSubscriptionCommand): CompletableFuture<SubscriptionId> =
        commandGateway.send(command)
}
