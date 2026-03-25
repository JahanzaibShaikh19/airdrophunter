package com.airdrophunter.service

import com.airdrophunter.domain.AirdropEntity
import com.airdrophunter.domain.AirdropStatus
import com.airdrophunter.repository.AirdropEntityRepository
import com.airdrophunter.repository.TelegramSubscriberRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class TelegramAlertService(
    private val subscriberRepo: TelegramSubscriberRepository,
    private val airdropRepo: AirdropEntityRepository,
    private val bot: AirdropHunterBot
) {
    private val log = LoggerFactory.getLogger(TelegramAlertService::class.java)

    // ── Immediate Triggers (Called by DefiLlamaService) ───────────────────────

    fun notifyNewAirdrops(airdrops: List<AirdropEntity>) {
        if (airdrops.isEmpty()) return
        
        val activeSubscribers = subscriberRepo.findAllByIsSubscribedTrue()
        if (activeSubscribers.isEmpty()) return

        airdrops.forEach { drop ->
            val message = formatAirdropMessage("🔥 NEW AIRDROP DETECTED", drop)
            activeSubscribers.forEach { sub ->
                bot.sendMessage(sub.chatId, message)
            }
        }
        log.info("Broadcasted ${airdrops.size} new airdrops to ${activeSubscribers.size} subscribers")
    }

    fun notifyHotAirdrops(airdrops: List<AirdropEntity>) {
        if (airdrops.isEmpty()) return
        
        val activeSubscribers = subscriberRepo.findAllByIsSubscribedTrue()
        if (activeSubscribers.isEmpty()) return

        airdrops.forEach { drop ->
            val message = formatAirdropMessage("🚀 FRESH HOT AIRDROP", drop)
            activeSubscribers.forEach { sub ->
                bot.sendMessage(sub.chatId, message)
            }
        }
        log.info("Broadcasted ${airdrops.size} hot airdrops to ${activeSubscribers.size} subscribers")
    }

    // ── Scheduled Jobs ────────────────────────────────────────────────────────

    /**
     * Runs every hour to find airdrops ending within exactly the next 24 hours.
     * We don't want to spam, so we find ones that cross the 24h threshold right now.
     */
    @Scheduled(cron = "0 0 * * * *") // Top of every hour
    @Transactional
    fun checkExpiringAirdrops() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val targetThreshold = now.plusHours(24)
        
        // Find airdrops whose deadline is between 23h and 24h from now,
        // and haven't been notified yet.
        val candidates = airdropRepo.findEndingSoon(AirdropStatus.LIVE, targetThreshold)
            .filter { it.deadline != null && it.deadline.isAfter(now.plusHours(23)) }
            .filter { it.notifiedDeadlineAt == null }

        if (candidates.isEmpty()) return

        val activeSubscribers = subscriberRepo.findAllByIsSubscribedTrue()
        
        candidates.forEach { drop ->
            val message = formatAirdropMessage("⏰ URGENT: ENDING IN 24H", drop)
            activeSubscribers.forEach { sub ->
                bot.sendMessage(sub.chatId, message)
            }
            
            // Mark as notified so we don't send again next hour
            airdropRepo.save(drop.copy(notifiedDeadlineAt = now))
        }
        
        log.info("Broadcasted ${candidates.size} expiring alerts to ${activeSubscribers.size} subscribers")
    }

    // ── Formatting ────────────────────────────────────────────────────────────

    private fun formatAirdropMessage(header: String, drop: AirdropEntity): String {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm 'UTC'")
        val deadlineStr = drop.deadline?.format(formatter) ?: "TBA"
        
        val stepsStr = if (drop.steps.isEmpty()) "None listed." else {
            drop.steps.withIndex().joinToString("\n") { (idx, step) -> "${idx + 1}\\. $step" }
        }

        // Markdown V1 escaping (basic markdown via Telegram)
        return """
            *$header*
            
            **Name:** ${drop.name} (${drop.symbol ?: "N/A"})
            **Category:** ${drop.category.name}
            **Est. Value:** ${'$'}${drop.estimatedValueMin} - ${'$'}${drop.estimatedValueMax}
            **Deadline:** $deadlineStr
            
            ✅ **Steps:**
            $stepsStr
            
            🔗 Check details: airdrophunter.io/${drop.llamaSlug ?: ""}
        """.trimIndent()
    }
}
