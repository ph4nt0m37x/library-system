package com.mcpservice.config

import jakarta.servlet.FilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityComponentsTest {
    @Test
    fun `audience validator accepts only intended tokens`() {
        assertTrue(AudienceValidator("library-api").validate(jwt(listOf("other"))).hasErrors())
        assertFalse(AudienceValidator("library-api").validate(jwt(listOf("library-api"))).hasErrors())
    }

    @Test
    fun `realm role converter maps Keycloak roles`() {
        val token = Jwt.withTokenValue("token")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .claim("realm_access", mapOf("roles" to listOf("library-user", "library-admin")))
            .build()

        assertEquals(
            setOf("ROLE_library-user", "ROLE_library-admin"),
            RealmRoleConverter().convert(token).map { it.authority }.toSet()
        )
    }

    @Test
    fun `origin filter blocks unknown MCP origins and allows no-origin CLI requests`() {
        val filter = OriginValidationFilter("http://localhost:6274")
        val blockedRequest = MockHttpServletRequest("POST", "/mcp").apply {
            addHeader("Origin", "https://attacker.example")
        }
        val blockedResponse = MockHttpServletResponse()
        filter.doFilter(blockedRequest, blockedResponse, NoOpFilterChain)
        assertEquals(403, blockedResponse.status)

        val cliResponse = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest("POST", "/mcp"), cliResponse, NoOpFilterChain)
        assertEquals(200, cliResponse.status)
    }

    private fun jwt(audience: List<String>) = Jwt.withTokenValue("token")
        .header("alg", "none")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .audience(audience)
        .build()

    private object NoOpFilterChain : FilterChain {
        override fun doFilter(request: jakarta.servlet.ServletRequest, response: jakarta.servlet.ServletResponse) = Unit
    }
}
