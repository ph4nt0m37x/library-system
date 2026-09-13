package com.apigateway.config

import com.apigateway.security.AudienceValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val issuerUri: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private val jwkSetUri: String,
    @Value("\${security.jwt.audience}")
    private val audience: String,
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { csrf -> csrf.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt(Customizer.withDefaults()) }
            .build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
        val issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri)
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                issuerValidator,
                AudienceValidator(audience),
            ),
        )
        return decoder
    }
}
