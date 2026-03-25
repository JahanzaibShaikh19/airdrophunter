package com.airdrophunter.service

import com.airdrophunter.domain.ProUser
import com.airdrophunter.repository.ProUserRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.airdrophunter.security.JwtService
import com.airdrophunter.security.JwtProperties
import java.time.OffsetDateTime
import java.util.Optional

@DisplayName("GumroadWebhookService")
class GumroadWebhookServiceTest {

    private val proUserService: ProUserService = mockk(relaxed = true)
    private lateinit var service: GumroadWebhookService

    private val VALID_SECRET = "my-gumroad-secret"

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = GumroadWebhookService(proUserService, VALID_SECRET)
    }

    // ── Signature Verification ────────────────────────────────────────────────

    @Nested
    @DisplayName("verifySignature()")
    inner class VerifySignature {

        @Test
        fun `accepts a correctly computed HMAC-SHA256 signature`() {
            val body = "email=buyer%40example.com&license_key=ABC-123&sale_id=sale_001".toByteArray()
            val expectedSig = computeHmac(body, VALID_SECRET)
            assertTrue(service.verifySignature(body, expectedSig))
        }

        @Test
        fun `rejects an incorrect signature`() {
            val body = "body".toByteArray()
            assertFalse(service.verifySignature(body, "wrong-signature"))
        }

        @Test
        fun `rejects a signature from a different secret`() {
            val body = "body".toByteArray()
            val sig = computeHmac(body, "other-secret")
            assertFalse(service.verifySignature(body, sig))
        }

        @Test
        fun `is case-insensitive for hex digits`() {
            val body = "test body".toByteArray()
            val sig = computeHmac(body, VALID_SECRET)
            assertTrue(service.verifySignature(body, sig.uppercase()))
        }

        @Test
        fun `rejects empty signature string`() {
            assertFalse(service.verifySignature("x".toByteArray(), ""))
        }

        @Test
        fun `blank secret bypasses verification (dev mode)`() {
            val devService = GumroadWebhookService(proUserService, "")
            assertTrue(devService.verifySignature("any".toByteArray(), "any-sig"))
        }

        // Helper
        private fun computeHmac(data: ByteArray, secret: String): String {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
            return mac.doFinal(data).joinToString("") { "%02x".format(it) }
        }
    }

    // ── processSaleEvent ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("processSaleEvent()")
    inner class ProcessSaleEvent {

        @Test
        fun `activates PRO for a valid sale event`() {
            val params = mapOf(
                "email" to "buyer@example.com",
                "license_key" to "LK-ABC-123",
                "sale_id" to "sale_001",
                "refunded" to "false"
            )
            val result = service.processSaleEvent(params)
            assertTrue(result.success)
            verify { proUserService.activateFromGumroad("buyer@example.com", "LK-ABC-123", "sale_001") }
        }

        @Test
        fun `deactivates PRO when refunded=true`() {
            val params = mapOf(
                "email" to "buyer@example.com",
                "license_key" to "LK-ABC-123",
                "sale_id" to "sale_001",
                "refunded" to "true"
            )
            val result = service.processSaleEvent(params)
            assertTrue(result.success)
            verify { proUserService.deactivate("buyer@example.com") }
            verify(exactly = 0) { proUserService.activateFromGumroad(any(), any(), any()) }
        }

        @Test
        fun `returns failure when email is missing`() {
            val result = service.processSaleEvent(mapOf("license_key" to "LK-1"))
            assertFalse(result.success)
            assertTrue(result.message.contains("email", ignoreCase = true))
            verify(exactly = 0) { proUserService.activateFromGumroad(any(), any(), any()) }
        }

        @Test
        fun `returns failure when license_key is missing (non-refund)`() {
            val params = mapOf("email" to "buyer@example.com", "refunded" to "false")
            val result = service.processSaleEvent(params)
            assertFalse(result.success)
            assertTrue(result.message.contains("license_key", ignoreCase = true))
        }

        @Test
        fun `handles refunded=TRUE case-insensitively`() {
            val params = mapOf("email" to "a@b.com", "refunded" to "TRUE")
            service.processSaleEvent(params)
            verify { proUserService.deactivate("a@b.com") }
        }

        @Test
        fun `normalises email to lowercase`() {
            val params = mapOf(
                "email" to "BUYER@EXAMPLE.COM",
                "license_key" to "K1",
                "sale_id" to "S1"
            )
            service.processSaleEvent(params)
            verify { proUserService.activateFromGumroad("buyer@example.com", "K1", "S1") }
        }

        @Test
        fun `passes null saleId when sale_id is absent`() {
            val params = mapOf("email" to "a@b.com", "license_key" to "K1")
            service.processSaleEvent(params)
            verify { proUserService.activateFromGumroad("a@b.com", "K1", null) }
        }
    }
}
