package com.airdrophunter.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class WalletRateLimitFilter : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.requestURI != "/api/wallet/check"

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val bucket = buckets.computeIfAbsent(resolveClientIp(request)) { createBucket() }
        val probe = bucket.tryConsumeAndReturnRemaining(1)

        response.setHeader("X-Rate-Limit-Limit", "10")
        response.setHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())

        if (probe.isConsumed) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpServletResponse.SC_TOO_MANY_REQUESTS
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write(
            "{\"error\":\"Rate limit exceeded. Maximum 10 wallet checks per hour per IP.\"}"
        )
    }

    private fun createBucket(): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.classic(
                10,
                Refill.intervally(10, Duration.ofHours(1))
            )
        )
        .build()

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        if (!forwardedFor.isNullOrBlank()) {
            return forwardedFor.substringBefore(',').trim()
        }

        val realIp = request.getHeader("X-Real-IP")
        if (!realIp.isNullOrBlank()) {
            return realIp.trim()
        }

        return request.remoteAddr ?: "unknown"
    }
}
