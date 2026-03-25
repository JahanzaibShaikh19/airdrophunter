package com.airdrophunter.service

import com.airdrophunter.config.TelegramConfig
import com.airdrophunter.domain.TelegramSubscriber
import com.airdrophunter.repository.TelegramSubscriberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class AirdropHunterBot(
    private val config: TelegramConfig,
    private val subscriberRepo: TelegramSubscriberRepository,
    private val proUserService: ProUserService,
    private val airdropService: AirdropService
) : TelegramLongPollingBot(config.token) {

    private val log = LoggerFactory.getLogger(AirdropHunterBot::class.java)

    override fun getBotUsername(): String = config.username

    override fun onUpdateReceived(update: Update) {
        if (!update.hasMessage() || !update.message.hasText()) return

        val chatId = update.message.chatId
        val text = update.message.text.trim()
        val parts = text.split("\\s+".toRegex())
        val command = parts[0].lowercase()

        val responseText = try {
            when (command) {
                "/start" -> handleStart(parts)
                "/link" -> handleLink(chatId, parts)
                "/alerts" -> handleAlerts(chatId, parts)
                "/check" -> handleCheck(parts)
                else -> "Unknown command. Try /start, /link <email>, /alerts on, or /check <address>."
            }
        } catch (ex: Exception) {
            log.error("Error processing command from $chatId: ${ex.message}", ex)
            "An error occurred while processing your request. Please try again later."
        }

        sendMessage(chatId, responseText)
    }

    private fun handleStart(parts: List<String>): String {
        return """
            🚀 Welcome to @AirdropHunterBot!
            
            We monitor the DeFi ecosystem for lucrative airdrops.
            
            **Available Commands:**
            `/link <email>` — Link your PRO account email
            `/alerts on` — Subscribe to real-time airdrop alerts (PRO only)
            `/alerts off` — Unsubscribe from alerts
            `/check <0x...>` — Check wallet eligibility
            
            To get started, please link your PRO account email using the `/link` command.
        """.trimIndent()
    }

    private fun handleLink(chatId: Long, parts: List<String>): String {
        if (parts.size < 2) {
            return "Please provide your email. Example: `/link myemail@example.com`"
        }
        val email = parts[1].lowercase()
        
        val subscriber = subscriberRepo.findByChatId(chatId)
            ?: TelegramSubscriber(chatId = chatId)

        subscriberRepo.save(subscriber.copy(email = email))
        
        return "✅ Your Telegram account has been linked to **$email**.\n\nType `/alerts on` to verify your PRO status and activate notifications."
    }

    private fun handleAlerts(chatId: Long, parts: List<String>): String {
        if (parts.size < 2 || (parts[1] != "on" && parts[1] != "off")) {
            return "Usage: `/alerts on` or `/alerts off`"
        }

        val subscriber = subscriberRepo.findByChatId(chatId)
        val email = subscriber?.email

        if (parts[1] == "off") {
            if (subscriber != null) {
                subscriberRepo.save(subscriber.copy(isSubscribed = false))
            }
            return "🔕 You have unsubscribed from Airdrop alerts."
        }

        // Handle "/alerts on"
        if (email == null) {
            return "⚠️ You must link an email before enabling alerts. Use `/link <email>`."
        }

        if (!proUserService.isProUser(email)) {
            return "🚫 **PRO Subscription Required**\n\nNo active PRO subscription found for `$email`. Upgrade at airdrophunter.io to receive real-time alerts."
        }

        subscriberRepo.save(subscriber.copy(isSubscribed = true))
        return "🔔 **Alerts Activated!**\n\nYou will now receive real-time notifications for NEW and HOT airdrops, plus 24-hour expiration warnings."
    }

    private fun handleCheck(parts: List<String>): String {
        if (parts.size < 2) {
            return "Usage: `/check <0x...address>`"
        }
        val address = parts[1]
        
        return runBlocking {
            try {
                val result = airdropService.checkWalletEligibility(address)
                val statusIcon = if (result.isEligible) "✅" else "❌"
                
                """
                🔍 **Wallet Check**
                `$address`
                
                $statusIcon ${result.message}
                """.trimIndent()
            } catch (ex: Exception) {
                "Error checking wallet: ${ex.message}"
            }
        }
    }

    fun sendMessage(chatId: Long, text: String) {
        val message = SendMessage()
        message.chatId = chatId.toString()
        message.text = text
        message.enableMarkdown(true)

        try {
            execute(message)
        } catch (e: TelegramApiException) {
            log.error("Failed to send message to $chatId", e)
        }
    }
}
