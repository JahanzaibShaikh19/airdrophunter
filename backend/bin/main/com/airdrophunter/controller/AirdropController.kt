package com.airdrophunter.controller

import com.airdrophunter.dto.AirdropDto
import com.airdrophunter.service.AirdropService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

private const val FREE_TIER_LIMIT = 4

@RestController
@RequestMapping("/api/airdrops")
@CrossOrigin(origins = ["*"])
class AirdropController(private val service: AirdropService) {

    /**
     * Free endpoint — caps the result at [FREE_TIER_LIMIT] airdrops.
     * Authenticated PRO users still hit this endpoint but are encouraged
     * to use /api/airdrops/pro for the full list.
     */
    @GetMapping
    fun getAllActive(): ResponseEntity<Map<String, Any>> = runBlocking {
        val all = service.getAllActive()
        val isPro = isProRequest()
        val preview = if (isPro) all else all.take(FREE_TIER_LIMIT)
        ResponseEntity.ok(
            mapOf(
                "airdrops" to preview,
                "total" to all.size,
                "shown" to preview.size,
                "pro" to isPro,
                "upgradeHint" to if (!isPro && all.size > FREE_TIER_LIMIT)
                    "${all.size - FREE_TIER_LIMIT} more airdrops available with PRO — activate at /api/auth/activate"
                else ""
            )
        )
    }

    /**
     * Public hot list — also capped at [FREE_TIER_LIMIT] for non-pro users.
     */
    @GetMapping("/hot")
    fun getHot(): ResponseEntity<Map<String, Any>> = runBlocking {
        val all = service.getHot()
        val isPro = isProRequest()
        val preview = if (isPro) all else all.take(FREE_TIER_LIMIT)
        ResponseEntity.ok(
            mapOf(
                "airdrops" to preview,
                "total" to all.size,
                "shown" to preview.size,
                "pro" to isPro
            )
        )
    }

    /**
     * PRO-only endpoint — returns the FULL airdrop list.
     * Access is enforced by SecurityConfig (requires ROLE_PRO).
     */
    @GetMapping("/pro")
    fun getAllPro(
        @AuthenticationPrincipal email: String
    ): ResponseEntity<Map<String, Any>> = runBlocking {
        val all = service.getAllActive()
        ResponseEntity.ok(
            mapOf(
                "airdrops" to all,
                "total" to all.size,
                "shown" to all.size,
                "pro" to true,
                "email" to email
            )
        )
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun isProRequest(): Boolean =
        SecurityContextHolder.getContext().authentication
            ?.authorities
            ?.contains(SimpleGrantedAuthority("ROLE_PRO")) == true
}
