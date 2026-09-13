package com.mcpservice

import com.mcpservice.mcp.MemberTools
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.cloud.consul.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.consul.discovery.register=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://127.0.0.1:1/jwks"
    ]
)
class McpServiceApplicationTests @Autowired constructor(
    private val memberTools: MemberTools
) {
    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun contextLoads() = Unit

    @Test
    fun `MCP endpoint requires a bearer token`() {
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port/mcp"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}"""
                )
            )
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding())
        assertEquals(401, response.statusCode())
    }

    @Test
    fun `authenticated client can initialize MCP protocol`() {
        Mockito.`when`(jwtDecoder.decode("valid-token")).thenReturn(
            Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject("mcp-smoke-client")
                .claim("aud", listOf("library-api"))
                .claim("realm_access", mapOf("roles" to listOf("library-admin")))
                .build()
        )

        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port/mcp"))
            .header("Authorization", "Bearer valid-token")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-11-25","capabilities":{},"clientInfo":{"name":"test","version":"1"}}}"""
                )
            )
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        assertEquals(200, response.statusCode())
        assertTrue(response.body().contains("library-system"))
    }

    @Test
    fun `method security protects sensitive tools before downstream access`() {
        assertFailsWith<AuthenticationCredentialsNotFoundException> {
            memberTools.getAccountSummary("00000000-0000-0000-0000-000000000000")
        }
    }
}
