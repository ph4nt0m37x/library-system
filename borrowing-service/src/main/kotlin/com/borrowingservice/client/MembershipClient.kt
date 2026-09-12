package com.borrowingservice.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "membership-service", configuration = [MembershipClientConfiguration::class])
interface MembershipClient {
    @GetMapping("/api/members/{memberId}/subscription-status")
    fun subscriptionEligibility(@PathVariable memberId: String): MembershipEligibilityResponse

    fun hasActiveSubscription(memberId: String): Boolean = subscriptionEligibility(memberId).active
}
