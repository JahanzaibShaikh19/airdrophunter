package com.airdrophunter.config

import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class WalletRateLimitFilterTest {

    private val filter = WalletRateLimitFilter()

    @Test
    fun `allows ten requests per IP and blocks the eleventh`() {
        repeat(10) {
            val response = invokeFilter(ip = "203.0.113.10")
            assertEquals(HttpServletResponse.SC_OK, response.status)
        }

        val blockedResponse = invokeFilter(ip = "203.0.113.10")

        assertEquals(HttpServletResponse.SC_TOO_MANY_REQUESTS, blockedResponse.status)
        assertEquals("application/json", blockedResponse.contentType)
    }

    @Test
    fun `does not rate limit other endpoints`() {
        val request = MockHttpServletRequest("GET", "/api/stats")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        assertEquals(HttpServletResponse.SC_OK, response.status)
    }

    private fun invokeFilter(ip: String): MockHttpServletResponse {
        val request = MockHttpServletRequest("POST", "/api/wallet/check")
        request.remoteAddr = ip
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())
        return response
    }
}
