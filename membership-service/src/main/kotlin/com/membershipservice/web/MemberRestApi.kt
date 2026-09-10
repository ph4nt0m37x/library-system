package com.membershipservice.web

import com.membershipservice.model.command.RegisterMemberCommand
import com.membershipservice.model.command.RenewSubscriptionCommand
import com.membershipservice.model.command.StartSubscriptionCommand
import com.membershipservice.model.command.UpdateMemberContactDetailsCommand
import com.membershipservice.model.command.UpdateMemberNameCommand
import com.membershipservice.model.valueObject.Email
import com.membershipservice.model.valueObject.MemberId
import com.membershipservice.model.valueObject.MembershipNumber
import com.membershipservice.model.valueObject.PhoneNumber
import com.membershipservice.model.valueObject.dto.MemberResponse
import com.membershipservice.model.valueObject.dto.RegisterMemberDTO
import com.membershipservice.model.valueObject.dto.RenewSubscriptionDTO
import com.membershipservice.model.valueObject.dto.StartSubscriptionDTO
import com.membershipservice.model.valueObject.dto.UpdateMemberContactDetailsDTO
import com.membershipservice.model.valueObject.dto.UpdateMemberNameDTO
import com.membershipservice.service.MemberService
import com.membershipservice.service.MemberViewReadService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.concurrent.CompletableFuture

data class CommandResponse(val id: String)

@RestController
@RequestMapping("/api/members")
class MemberRestApi(
    private val memberService: MemberService,
    private val memberViewReadService: MemberViewReadService
) {
    @Operation(summary = "Get all members")
    @GetMapping("/all")
    fun findAllMembers(): ResponseEntity<List<MemberResponse>> =
        ResponseEntity.ok(memberViewReadService.findAll())

    @Operation(summary = "Get member by ID")
    @GetMapping("/{id}")
    fun findMemberById(@PathVariable id: String): ResponseEntity<MemberResponse> =
        memberViewReadService.findById(MemberId(id))
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @Operation(summary = "Get member by membership number")
    @GetMapping("/membership-number/{membershipNumber}")
    fun findMemberByMembershipNumber(
        @PathVariable membershipNumber: String
    ): ResponseEntity<MemberResponse> =
        memberViewReadService.findByMembershipNumber(MembershipNumber(membershipNumber))
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()

    @Operation(summary = "Check whether a member has an active subscription")
    @GetMapping("/{memberId}/subscription-status")
    fun hasActiveSubscription(@PathVariable memberId: String): ResponseEntity<Boolean> =
        ResponseEntity.ok(memberViewReadService.hasActiveSubscription(MemberId(memberId)))

    @Operation(summary = "Register a member")
    @PostMapping("/register")
    fun registerMember(@RequestBody dto: RegisterMemberDTO): CompletableFuture<ResponseEntity<CommandResponse>> =
        memberService.registerMember(
            RegisterMemberCommand(
                firstName = dto.firstName,
                lastName = dto.lastName,
                email = Email(dto.email),
                phoneNumber = PhoneNumber(dto.phoneNumber)
            )
        ).thenApply { id ->
            ResponseEntity.created(URI.create("/api/members/${id.baseValue()}"))
                .body(CommandResponse(id.baseValue()))
        }

    @Operation(summary = "Update a member's name")
    @PutMapping("/{id}/name")
    fun updateMemberName(
        @PathVariable id: String,
        @RequestBody dto: UpdateMemberNameDTO
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        memberService.updateMemberName(
            UpdateMemberNameCommand(MemberId(id), dto.firstName, dto.lastName)
        ).thenApply { ResponseEntity.ok(CommandResponse(it.baseValue())) }

    @Operation(summary = "Update a member's contact details")
    @PutMapping("/{id}/contact-details")
    fun updateMemberContactDetails(
        @PathVariable id: String,
        @RequestBody dto: UpdateMemberContactDetailsDTO
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        memberService.updateMemberContactDetails(
            UpdateMemberContactDetailsCommand(
                MemberId(id),
                Email(dto.email),
                PhoneNumber(dto.phoneNumber)
            )
        ).thenApply { ResponseEntity.ok(CommandResponse(it.baseValue())) }

    @Operation(summary = "Start a member's first subscription")
    @PostMapping("/{id}/subscriptions/start")
    fun startSubscription(
        @PathVariable id: String,
        @RequestBody dto: StartSubscriptionDTO
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        memberService.startSubscription(
            StartSubscriptionCommand(
                MemberId(id), dto.tier, dto.startsAt,
                dto.amountPaid, dto.currency, dto.paidAt
            )
        ).thenApply { ResponseEntity.ok(CommandResponse(it.baseValue())) }

    @Operation(summary = "Renew a member's subscription")
    @PostMapping("/{id}/subscriptions/renew")
    fun renewSubscription(
        @PathVariable id: String,
        @RequestBody dto: RenewSubscriptionDTO
    ): CompletableFuture<ResponseEntity<CommandResponse>> =
        memberService.renewSubscription(
            RenewSubscriptionCommand(
                MemberId(id), dto.tier,
                dto.amountPaid, dto.currency, dto.paidAt
            )
        ).thenApply { ResponseEntity.ok(CommandResponse(it.baseValue())) }
}
