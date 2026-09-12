package com.borrowingservice.client

import feign.Response
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean

class MembershipClientConfiguration {
    @Bean
    fun membershipErrorDecoder(): ErrorDecoder = MembershipClientErrorDecoder()
}

private class MembershipClientErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception =
        if (response.status() == 404) {
            MembershipMemberNotFoundException()
        } else {
            MembershipServiceUnavailableException()
        }
}
