package com.airdrophunter.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    /** Base64-encoded HMAC-SHA512 secret (min 512 bits). Set via JWT_SECRET env var. */
    var secret: String = "",

    /** Token lifetime in milliseconds. Default: 30 days. */
    var expirationMs: Long = 30L * 24 * 60 * 60 * 1000
)
