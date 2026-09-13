package com.mcpservice.client

import com.mcpservice.client.dto.MemberWire
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(name = "membership-service")
interface MembershipClient {
    @GetMapping("/api/members/{id}")
    fun findById(@PathVariable("id") id: String): MemberWire
}
