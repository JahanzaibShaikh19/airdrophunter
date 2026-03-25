package com.airdrophunter.service

import com.airdrophunter.config.TelegramConfig
import com.airdrophunter.domain.TelegramSubscriber
import com.airdrophunter.repository.TelegramSubscriberRepository
import com.airdrophunter.dto.WalletCheckResponse
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

@DisplayName("AirdropHunterBot")
class AirdropHunterBotTest {

    private val subscriberRepo: TelegramSubscriberRepository = mockk(relaxed = true)
    private val proUserService: ProUserService = mockk(relaxed = true)
    private val airdropService: AirdropService = mockk(relaxed = true)

    private val config = TelegramConfig(username = "TestBot", token = "123:ABC")
    
    // We spy on the bot to intercept execute() so we don't actually hit Telegram APIs
    private lateinit var bot: AirdropHunterBot

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        bot = spyk(AirdropHunterBot(config, subscriberRepo, proUserService, airdropService))
        every { bot.execute(any<SendMessage>()) } returns Message() // Mock Telegram API call
    }

    private fun mockUpdate(chatId: Long, text: String): Update {
        val message = mockk<Message>(relaxed = true)
        every { message.chatId } returns chatId
        every { message.hasText() } returns true
        every { message.text } returns text

        val update = mockk<Update>(relaxed = true)
        every { update.hasMessage() } returns true
        every { update.message } returns message
        return update
    }

    @Nested
    @DisplayName("Command Routing")
    inner class Routing {

        @Test
        fun `/start replies with welcome`() {
            bot.onUpdateReceived(mockUpdate(1L, "/start"))
            verify { bot.sendMessage(1L, match { it.contains("Welcome to @AirdropHunterBot") }) }
        }

        @Test
        fun `unknown command replies with fallback`() {
            bot.onUpdateReceived(mockUpdate(1L, "/ghost"))
            verify { bot.sendMessage(1L, match { it.contains("Unknown command") }) }
        }
    }

    @Nested
    @DisplayName("/link command")
    inner class LinkCommand {

        @Test
        fun `requires an email argument`() {
            bot.onUpdateReceived(mockUpdate(1L, "/link"))
            verify { bot.sendMessage(1L, match { it.contains("Please provide your email") }) }
            verify(exactly = 0) { subscriberRepo.save(any()) }
        }

        @Test
        fun `creates new subscriber and links email`() {
            every { subscriberRepo.findByChatId(1L) } returns null
            bot.onUpdateReceived(mockUpdate(1L, "/link test@example.com"))

            verify { subscriberRepo.save(match { it.chatId == 1L && it.email == "test@example.com" }) }
            verify { bot.sendMessage(1L, match { it.contains("linked to **test@example.com**") }) }
        }
    }

    @Nested
    @DisplayName("/alerts command")
    inner class AlertsCommand {

        private val subLinked = TelegramSubscriber(chatId = 1L, email = "pro@ex.com")
        private val subUnlinked = TelegramSubscriber(chatId = 2L, email = null)

        @Test
        fun `requires email link before enabling`() {
            every { subscriberRepo.findByChatId(2L) } returns subUnlinked
            bot.onUpdateReceived(mockUpdate(2L, "/alerts on"))
            verify { bot.sendMessage(2L, match { it.contains("must link an email before enabling alerts") }) }
        }

        @Test
        fun `denies if not a PRO user`() {
            every { subscriberRepo.findByChatId(1L) } returns subLinked
            every { proUserService.isProUser("pro@ex.com") } returns false

            bot.onUpdateReceived(mockUpdate(1L, "/alerts on"))

            verify { bot.sendMessage(1L, match { it.contains("No active PRO subscription found") }) }
            verify(exactly = 0) { subscriberRepo.save(any()) }
        }

        @Test
        fun `enables alerts if PRO user`() {
            every { subscriberRepo.findByChatId(1L) } returns subLinked
            every { proUserService.isProUser("pro@ex.com") } returns true

            bot.onUpdateReceived(mockUpdate(1L, "/alerts on"))

            verify { subscriberRepo.save(match { it.isSubscribed }) }
            verify { bot.sendMessage(1L, match { it.contains("Alerts Activated") }) }
        }

        @Test
        fun `turns off alerts`() {
            every { subscriberRepo.findByChatId(1L) } returns subLinked.copy(isSubscribed = true)
            bot.onUpdateReceived(mockUpdate(1L, "/alerts off"))
            verify { subscriberRepo.save(match { !it.isSubscribed }) }
            verify { bot.sendMessage(1L, match { it.contains("unsubscribed") }) }
        }
    }

    @Nested
    @DisplayName("/check command")
    inner class CheckCommand {

        @Test
        fun `checks wallet via AirdropService`() {
            coEvery { airdropService.checkWalletEligibility("0xABC") } returns 
                WalletCheckResponse("0xABC", true, "Eligible")

            bot.onUpdateReceived(mockUpdate(3L, "/check 0xABC"))

            verify { bot.sendMessage(3L, match { it.contains("✅ Eligible") }) }
        }
    }
}
