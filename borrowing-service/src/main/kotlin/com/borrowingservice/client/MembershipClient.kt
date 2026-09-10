package com.borrowingservice.client

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "membership-service")
interface MembershipClient {
    @GetMapping("/api/members/{memberId}/subscription-status")
    fun hasActiveSubscription(@PathVariable memberId: String): Boolean
}
