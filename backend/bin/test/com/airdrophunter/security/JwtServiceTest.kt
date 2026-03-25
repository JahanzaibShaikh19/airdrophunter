package com.airdrophunter.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("JwtService")
class JwtServiceTest {

    private lateinit var service: JwtService

    private val testProps = JwtProperties(
        secret = "test-secret-key-that-is-long-enough-for-hs512-algorithm-padding",
        expirationMs = 3_600_000L   // 1 hour
    )

    @BeforeEach
    fun setUp() { service = JwtService(testProps) }

    // ── Token Generation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken()")
    inner class GenerateToken {

        @Test
        fun `generates a non-blank token`() {
            val token = service.generateToken("user@example.com", isPro = true)
            assertTrue(token.isNotBlank())
        }

        @Test
        fun `generated token has three JWT segments`() {
            val token = service.generateToken("user@example.com", isPro = false)
            assertEquals(3, token.split(".").size)
        }

        @Test
        fun `pro token contains ROLE_PRO claim`() {
            val token = service.generateToken("pro@example.com", isPro = true)
            assertEquals(JwtService.ROLE_PRO, service.extractRole(token))
        }

        @Test
        fun `free token contains ROLE_FREE claim`() {
            val token = service.generateToken("free@example.com", isPro = false)
            assertEquals(JwtService.ROLE_FREE, service.extractRole(token))
        }

        @Test
        fun `tokens for different emails are distinct`() {
            val t1 = service.generateToken("a@x.com", true)
            val t2 = service.generateToken("b@x.com", true)
            assertNotEquals(t1, t2)
        }
    }

    // ── Email Extraction ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractEmail()")
    inner class ExtractEmail {

        @Test
        fun `extracts the correct email from a valid token`() {
            val email = "pro@example.com"
            val token = service.generateToken(email, isPro = true)
            assertEquals(email, service.extractEmail(token))
        }

        @Test
        fun `returns null for a malformed token`() {
            assertNull(service.extractEmail("not.a.jwt"))
        }

        @Test
        fun `returns null for a blank string`() {
            assertNull(service.extractEmail(""))
        }
    }

    // ── Role Extraction ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractRole()")
    inner class ExtractRole {

        @Test
        fun `returns ROLE_PRO for pro token`() {
            val token = service.generateToken("x@y.com", isPro = true)
            assertEquals(JwtService.ROLE_PRO, service.extractRole(token))
        }

        @Test
        fun `returns ROLE_FREE for free token`() {
            val token = service.generateToken("x@y.com", isPro = false)
            assertEquals(JwtService.ROLE_FREE, service.extractRole(token))
        }

        @Test
        fun `returns null for malformed token`() {
            assertNull(service.extractRole("garbage"))
        }
    }

    // ── isProToken ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isProToken()")
    inner class IsProToken {

        @Test
        fun `returns true for pro token`() {
            assertTrue(service.isProToken(service.generateToken("a@b.com", true)))
        }

        @Test
        fun `returns false for free token`() {
            assertFalse(service.isProToken(service.generateToken("a@b.com", false)))
        }

        @Test
        fun `returns false for malformed token`() {
            assertFalse(service.isProToken("bad.token"))
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isValid()")
    inner class IsValid {

        @Test
        fun `returns true for a fresh valid token`() {
            val token = service.generateToken("valid@test.com", true)
            assertTrue(service.isValid(token))
        }

        @Test
        fun `returns false for a garbled token`() {
            assertFalse(service.isValid("eyJhbGciOi.GARBAGE.here"))
        }

        @Test
        fun `returns false for expired token`() {
            val expiredProps = JwtProperties(
                secret = testProps.secret,
                expirationMs = -1000L     // already expired
            )
            val expiredService = JwtService(expiredProps)
            val token = expiredService.generateToken("x@y.com", true)
            assertFalse(service.isValid(token))
        }

        @Test
        fun `returns false for token signed with a different secret`() {
            val otherService = JwtService(JwtProperties(
                secret = "completely-different-secret-that-is-long-enough-for-hs512",
                expirationMs = 3_600_000L
            ))
            val foreign = otherService.generateToken("x@y.com", true)
            assertFalse(service.isValid(foreign))
        }
    }

    // ── Expiry Detection ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("isExpired()")
    inner class IsExpired {

        @Test
        fun `returns false for a fresh token`() {
            val token = service.generateToken("x@y.com", true)
            assertFalse(service.isExpired(token))
        }

        @Test
        fun `returns true for an actually expired token`() {
            val expired = JwtService(JwtProperties(secret = testProps.secret, expirationMs = -1000L))
            val token = expired.generateToken("x@y.com", true)
            assertTrue(service.isExpired(token))
        }
    }

    // ── Expiry Extraction ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractExpiry()")
    inner class ExtractExpiry {

        @Test
        fun `expiry is in the future for a fresh token`() {
            val token = service.generateToken("x@y.com", true)
            val expiry = service.extractExpiry(token)
            assertNotNull(expiry)
            assertTrue(expiry!!.after(java.util.Date()))
        }

        @Test
        fun `returns null for a malformed token`() {
            assertNull(service.extractExpiry("not.a.valid.jwt"))
        }
    }
}
