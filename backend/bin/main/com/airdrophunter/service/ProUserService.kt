package com.airdrophunter.service

import com.airdrophunter.domain.ProUser
import com.airdrophunter.repository.ProUserRepository
import com.airdrophunter.security.JwtService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Core business logic for PRO user lifecycle:
 * - Activation from Gumroad webhook or manual activation endpoint
 * - JWT issuance via [JwtService]
 * - Deactivation for refunds
 */
@Service
class ProUserService(
    private val repo: ProUserRepository,
    private val jwtService: JwtService
) {
    private val log = LoggerFactory.getLogger(ProUserService::class.java)

    // ── Activation ────────────────────────────────────────────────────────────

    /**
     * Called by the Gumroad webhook after a verified purchase.
     * Idempotent: re-activates a previously deactivated user with the same email.
     */
    @Transactional
    fun activateFromGumroad(email: String, licenseKey: String, saleId: String?): ProUser {
        // De-duplicate by Gumroad sale ID to avoid double-processing retried webhooks
        if (saleId != null && repo.existsByGumroadSaleId(saleId)) {
            log.info("Gumroad sale $saleId already processed — skipping duplicate")
            return repo.findByEmail(email).orElseThrow()
        }

        val existing = repo.findByEmail(email)
        return if (existing.isPresent) {
            val user = existing.get().copy(
                licenseKey = licenseKey,
                isActive = true,
                activatedAt = OffsetDateTime.now(ZoneOffset.UTC),
                gumroadSaleId = saleId
            )
            repo.save(user).also { log.info("Re-activated PRO user: $email") }
        } else {
            repo.save(
                ProUser(
                    email = email.lowercase().trim(),
                    licenseKey = licenseKey,
                    isActive = true,
                    activatedAt = OffsetDateTime.now(ZoneOffset.UTC),
                    gumroadSaleId = saleId
                )
            ).also { log.info("Activated new PRO user: $email") }
        }
    }

    /**
     * Called by [com.airdrophunter.controller.AuthController] — verifies the license key
     * matches the email in our DB, then returns a signed JWT.
     *
     * @throws IllegalArgumentException if credentials are invalid or user is inactive.
     */
    @Transactional(readOnly = true)
    fun activateAndIssueToken(email: String, licenseKey: String): String {
        val normalised = email.lowercase().trim()
        val user = repo.findByEmail(normalised)
            .orElseThrow { IllegalArgumentException("No PRO account found for email: $normalised") }

        if (!user.isActive) throw IllegalArgumentException("PRO account for $normalised is inactive")
        if (user.licenseKey != licenseKey) throw IllegalArgumentException("Invalid license key")

        log.info("Issuing JWT for PRO user: $normalised")
        return jwtService.generateToken(normalised, isPro = true)
    }

    // ── Deactivation (refund / chargeback) ───────────────────────────────────

    @Transactional
    fun deactivate(email: String) {
        val user = repo.findByEmail(email.lowercase().trim())
        if (user.isPresent) {
            repo.save(user.get().copy(isActive = false))
            log.info("Deactivated PRO user: $email")
        } else {
            log.warn("Deactivate called for unknown email: $email")
        }
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    fun isProUser(email: String): Boolean =
        repo.existsByEmailAndIsActiveTrue(email.lowercase().trim())

    fun findByEmail(email: String): ProUser? =
        repo.findByEmail(email.lowercase().trim()).orElse(null)
}
