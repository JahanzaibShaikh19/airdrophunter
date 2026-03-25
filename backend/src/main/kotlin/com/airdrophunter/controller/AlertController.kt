package com.airdrophunter.controller

import com.airdrophunter.service.ProUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class AlertConfig(
    val telegramBotUsername: String,
    val telegramDeepLink: String,
    val alertTypes: List<String>,
    val instructions: List<String>
)

data class ProStatusResponse(
    val email: String,
    val isPro: Boolean,
    val features: List<String>
)

/**
 * PRO-only alert & account endpoints.
 * Requires ROLE_PRO (enforced by SecurityConfig).
 *
 * GET /api/alerts         — returns Telegram alert setup instructions
 * GET /api/alerts/status  — returns the current user's PRO status
 */
@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = ["*"])
class AlertController(private val proUserService: ProUserService) {

    @GetMapping
    fun getAlertConfig(
        @AuthenticationPrincipal email: String
    ): ResponseEntity<AlertConfig> {
        val config = AlertConfig(
            telegramBotUsername = "AirdropHunterBot",
            telegramDeepLink = "https://t.me/AirdropHunterBot?start=${email.hashCode().toUInt()}",
            alertTypes = listOf(
                "New airdrop listed",
                "Airdrop ending in 48 hours",
                "High-value (>\$2,000) opportunity detected",
                "Wallet eligibility update"
            ),
            instructions = listOf(
                "1. Open Telegram and search for @AirdropHunterBot",
                "2. Click the deep link above or send /start to the bot",
                "3. Confirm your email in the bot chat",
                "4. Select the alert types you want to receive",
                "5. Alerts will be delivered in real-time as airdrops are tracked"
            )
        )
        return ResponseEntity.ok(config)
    }

    @GetMapping("/status")
    fun getProStatus(
        @AuthenticationPrincipal email: String
    ): ResponseEntity<ProStatusResponse> {
        val isPro = proUserService.isProUser(email)
        return ResponseEntity.ok(
            ProStatusResponse(
                email = email,
                isPro = isPro,
                features = if (isPro) listOf(
                    "Unlimited airdrops access",
                    "Telegram real-time alerts",
                    "Wallet eligibility tracking",
                    "Early access to new listings",
                    "Pro airdrop filter (isHot + category)"
                ) else listOf(
                    "4 airdrops preview",
                    "Basic stats dashboard"
                )
            )
        )
    }
}
