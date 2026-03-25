package com.airdrophunter.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.util.Date

/**
 * Stateless JWT service using HMAC-SHA512 signing.
 *
 * Token claims:
 *  - sub   : user email
 *  - role  : "ROLE_PRO" | "ROLE_FREE"
 *  - iat   : issued-at
 *  - exp   : expiry
 */
@Service
class JwtService(private val props: JwtProperties) {

    private val log = LoggerFactory.getLogger(JwtService::class.java)

    private val signingKey by lazy {
        // Pad or hash to ensure at least 512-bit key for HS512
        val raw = props.secret.padEnd(64, '0').toByteArray(StandardCharsets.UTF_8)
        Keys.hmacShaKeyFor(raw)
    }

    companion object {
        const val CLAIM_ROLE = "role"
        const val ROLE_PRO   = "ROLE_PRO"
        const val ROLE_FREE  = "ROLE_FREE"
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    fun generateToken(email: String, isPro: Boolean): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(email)
            .claim(CLAIM_ROLE, if (isPro) ROLE_PRO else ROLE_FREE)
            .issuedAt(Date(now))
            .expiration(Date(now + props.expirationMs))
            .signWith(signingKey)
            .compact()
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    /**
     * Returns true if the token is well-formed, correctly signed, and not expired.
     */
    fun isValid(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    /**
     * @throws JwtException if the token is invalid or expired.
     */
    fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun extractEmail(token: String): String? = runCatching {
        parseClaims(token).subject
    }.getOrElse {
        log.debug("extractEmail failed: ${it.message}")
        null
    }

    fun extractRole(token: String): String? = runCatching {
        parseClaims(token)[CLAIM_ROLE] as? String
    }.getOrElse { null }

    fun isProToken(token: String): Boolean = extractRole(token) == ROLE_PRO

    fun extractExpiry(token: String): Date? = runCatching {
        parseClaims(token).expiration
    }.getOrElse { null }

    /**
     * Returns true when the token has a valid signature but is past its expiry.
     * Used to provide a better error response vs. a completely malformed token.
     */
    fun isExpired(token: String): Boolean = runCatching {
        parseClaims(token)
        false
    }.getOrElse { it is ExpiredJwtException }
}
