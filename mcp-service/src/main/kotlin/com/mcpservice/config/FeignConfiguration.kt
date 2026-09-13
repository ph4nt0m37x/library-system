package com.mcpservice.config

import feign.RequestInterceptor
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

@Configuration
class FeignConfiguration {
    @Bean
    fun securityAndCorrelationHeaders(): RequestInterceptor = RequestInterceptor { template ->
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication is JwtAuthenticationToken) {
            template.header("Authorization", "Bearer ${authentication.token.tokenValue}")
        }

        val correlationId = MDC.get("correlationId") ?: UUID.randomUUID().toString()
        template.header("X-Correlation-ID", correlationId)
    }
}
