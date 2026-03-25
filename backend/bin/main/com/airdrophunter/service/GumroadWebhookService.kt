package com.airdrophunter.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Handles Gumroad webhook verification and event parsing.
 *
 * Gumroad sends a POST with `application/x-www-form-urlencoded` body and an
 * `X-Gumroad-Signature` header containing the HMAC-SHA256 hexdigest of the
 * raw request body keyed with the seller's Gumroad application secret.
 */
@Service
class GumroadWebhookService(
    private val proUserService: ProUserService,
    @Value("\${gumroad.webhook-secret:}") private val webhookSecret: String
) {
    private val log = LoggerFactory.getLogger(GumroadWebhookService::class.java)

    companion object {
        private const val HMAC_ALGO = "HmacSHA256"
    }

    // ── Signature Verification ────────────────────────────────────────────────

    /**
     * Verifies the Gumroad HMAC-SHA256 signature.
     * Constant-time comparison prevents timing attacks.
     *
     * @param rawBody raw request body bytes
     * @param signature value of X-Gumroad-Signature header
     * @return true if signature matches
     */
    fun verifySignature(rawBody: ByteArray, signature: String): Boolean {
        if (webhookSecret.isBlank()) {
            log.warn("GUMROAD_WEBHOOK_SECRET is not configured — skipping signature verification")
            return true   // Dev mode passthrough; should never happen in production
        }
        return try {
            val mac = Mac.getInstance(HMAC_ALGO)
            mac.init(SecretKeySpec(webhookSecret.toByteArray(Charsets.UTF_8), HMAC_ALGO))
            val computed = mac.doFinal(rawBody).joinToString("") { "%02x".format(it) }
            constantTimeEquals(computed, signature.lowercase())
        } catch (ex: Exception) {
            log.error("Signature verification error: ${ex.message}")
            false
        }
    }

    /** Timing-safe string comparison */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    // ── Event Processing ──────────────────────────────────────────────────────

    /**
     * Parses a Gumroad `application/x-www-form-urlencoded` sale event
     * and activates the buyer's PRO account.
     *
     * Expected fields (from Gumroad ping docs):
     *  - email              : buyer email
     *  - license_key        : generated license key
     *  - sale_id            : unique sale identifier (used for de-duplication)
     *  - refunded           : "true" if this is a refund notification
     *
     * @param params decoded form parameters from the webhook body
     */
    fun processSaleEvent(params: Map<String, String>): WebhookResult {
        val email      = params["email"]?.lowercase()?.trim()
        val licenseKey = params["license_key"]?.trim()
        val saleId     = params["sale_id"]?.trim()
        val refunded   = params["refunded"]?.equals("true", ignoreCase = true) ?: false

        if (email.isNullOrBlank()) {
            log.warn("Gumroad event missing 'email' field — ignored")
            return WebhookResult(success = false, message = "Missing email in payload")
        }

        if (refunded) {
            log.info("Gumroad refund for $email — deactivating PRO")
            proUserService.deactivate(email)
            return WebhookResult(success = true, message = "PRO deactivated (refund)")
        }

        if (licenseKey.isNullOrBlank()) {
            log.warn("Gumroad sale event for $email missing license_key — ignored")
            return WebhookResult(success = false, message = "Missing license_key in payload")
        }

        proUserService.activateFromGumroad(email, licenseKey, saleId)
        return WebhookResult(success = true, message = "PRO activated for $email")
    }
}

data class WebhookResult(val success: Boolean, val message: String)
