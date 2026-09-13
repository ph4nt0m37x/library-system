package com.mcpservice.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URI

@Component
class OriginValidationFilter(
    @Value("\${mcp.allowed-origins:}") configuredOrigins: String
) : OncePerRequestFilter() {
    private val allowedOrigins = configuredOrigins
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(::canonicalOrigin)
        .toSet()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI != "/mcp" && !request.requestURI.startsWith("/mcp/")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val origin = request.getHeader("Origin")
        if (origin != null && runCatching { canonicalOrigin(origin) }.getOrNull() !in allowedOrigins) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Origin is not allowed")
            return
        }
        filterChain.doFilter(request, response)
    }

    private fun canonicalOrigin(value: String): String {
        val uri = URI(value)
        require(uri.scheme == "http" || uri.scheme == "https") { "Origin must use HTTP or HTTPS" }
        require(!uri.host.isNullOrBlank()) { "Origin must include a host" }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) { "Invalid origin" }

        val defaultPort = if (uri.scheme == "https") 443 else 80
        val port = if (uri.port == -1) defaultPort else uri.port
        return "${uri.scheme.lowercase()}://${uri.host.lowercase()}:$port"
    }
}
