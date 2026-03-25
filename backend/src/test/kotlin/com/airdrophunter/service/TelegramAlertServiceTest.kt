package com.airdrophunter.service

import com.airdrophunter.domain.AirdropEntity
import com.airdrophunter.domain.AirdropStatus
import com.airdrophunter.domain.AirdropCategory
import com.airdrophunter.domain.TelegramSubscriber
import com.airdrophunter.repository.AirdropEntityRepository
import com.airdrophunter.repository.TelegramSubscriberRepository
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("TelegramAlertService")
class TelegramAlertServiceTest {

    private val subscriberRepo: TelegramSubscriberRepository = mockk()
    private val airdropRepo: AirdropEntityRepository = mockk()
    private val bot: AirdropHunterBot = mockk(relaxed = true)

    private lateinit var alertService: TelegramAlertService

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    private val sampleDrop = AirdropEntity(
        id = 1L, name = "LayerZero", symbol = "ZRO",
        estimatedValueMin = BigDecimal("100"), estimatedValueMax = BigDecimal("500"),
        status = AirdropStatus.LIVE, category = AirdropCategory.BRIDGE,
        steps = listOf("Step 1", "Step 2")
    )

    private val subscribers = listOf(
        TelegramSubscriber(chatId = 1L, isSubscribed = true),
        TelegramSubscriber(chatId = 2L, isSubscribed = true)
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        alertService = TelegramAlertService(subscriberRepo, airdropRepo, bot)
    }

    @Nested
    @DisplayName("notifyNewAirdrops()")
    inner class NotifyNew {

        @Test
        fun `broadcasts to all active subscribers`() {
            every { subscriberRepo.findAllByIsSubscribedTrue() } returns subscribers

            alertService.notifyNewAirdrops(listOf(sampleDrop))

            verify(exactly = 1) { bot.sendMessage(1L, match { it.contains("NEW AIRDROP DETECTED") }) }
            verify(exactly = 1) { bot.sendMessage(2L, match { it.contains("NEW AIRDROP DETECTED") }) }
            verify(exactly = 1) { bot.sendMessage(any(), match { it.contains("LayerZero") }) }
        }

        @Test
        fun `does nothing if no airdrops`() {
            alertService.notifyNewAirdrops(emptyList())
            verify(exactly = 0) { subscriberRepo.findAllByIsSubscribedTrue() }
        }

        @Test
        fun `does nothing if no active subscribers`() {
            every { subscriberRepo.findAllByIsSubscribedTrue() } returns emptyList()
            alertService.notifyNewAirdrops(listOf(sampleDrop))
            verify(exactly = 0) { bot.sendMessage(any(), any()) }
        }
    }

    @Nested
    @DisplayName("checkExpiringAirdrops()")
    inner class CheckExpiring {

        @Test
        fun `notifies for airdrops ending between 23h and 24h`() {
            // Deadline exactly 23.5 hours away
            val expiringDrop = sampleDrop.copy(deadline = now.plusMinutes((23.5 * 60).toLong()), notifiedDeadlineAt = null)
            
            every { airdropRepo.findEndingSoon(any(), any()) } returns listOf(expiringDrop)
            every { subscriberRepo.findAllByIsSubscribedTrue() } returns subscribers
            every { airdropRepo.save(any()) } answers { firstArg() }

            alertService.checkExpiringAirdrops()

            verify { bot.sendMessage(1L, match { it.contains("ENDING IN 24H") }) }
            // Verifies the notification flag was set and saved
            verify { airdropRepo.save(match { it.id == 1L && it.notifiedDeadlineAt != null }) }
        }

        @Test
        fun `skips airdrops ending sooner than 23 hours`() {
            // Already notified in a previous run, or just crossing into < 23h
            val tooSoon = sampleDrop.copy(deadline = now.plusHours(10))
            every { airdropRepo.findEndingSoon(any(), any()) } returns listOf(tooSoon)

            alertService.checkExpiringAirdrops()

            verify(exactly = 0) { bot.sendMessage(any(), any()) }
        }

        @Test
        fun `skips already notified airdrops`() {
            val alreadyNotified = sampleDrop.copy(
                deadline = now.plusHours(23).plusMinutes(30),
                notifiedDeadlineAt = now.minusHours(1)
            )
            every { airdropRepo.findEndingSoon(any(), any()) } returns listOf(alreadyNotified)

            alertService.checkExpiringAirdrops()

            verify(exactly = 0) { bot.sendMessage(any(), any()) }
        }
    }
}
