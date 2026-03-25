package com.airdrophunter.service

import com.airdrophunter.domain.Airdrop
import com.airdrophunter.repository.AirdropRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

class AirdropServiceTest {

    private val repo: AirdropRepository = mockk()
    private lateinit var service: AirdropService

    // ── Fixture helpers ──────────────────────────────────────────────────────

    private fun airdrop(
        id: Long = 1L,
        name: String = "Test Airdrop",
        token: String = "TEST",
        value: BigDecimal = BigDecimal("500.00"),
        endsAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5),
        isActive: Boolean = true
    ) = Airdrop(
        id = id,
        name = name,
        description = "A test airdrop description",
        token = token,
        protocol = "TestProtocol",
        chain = "Ethereum",
        estimatedValue = value,
        endsAt = endsAt,
        isActive = isActive,
        websiteUrl = "https://example.com",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    @BeforeEach
    fun setUp() {
        service = AirdropService(repo)
    }

    // ── getAllActive ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllActive()")
    inner class GetAllActive {

        @Test
        fun `returns mapped DTOs in repo order`() = runTest {
            val airdrops = listOf(airdrop(id = 1), airdrop(id = 2, name = "Second Drop"))
            every { repo.findAllByIsActiveTrueOrderByCreatedAtDesc() } returns airdrops

            val result = service.getAllActive()

            assertEquals(2, result.size)
            assertEquals("Test Airdrop", result[0].name)
            assertEquals("Second Drop", result[1].name)
        }

        @Test
        fun `returns empty list when no active airdrops`() = runTest {
            every { repo.findAllByIsActiveTrueOrderByCreatedAtDesc() } returns emptyList()

            val result = service.getAllActive()

            assertTrue(result.isEmpty())
        }

        @Test
        fun `maps all DTO fields correctly`() = runTest {
            val endsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7)
            val drop = airdrop(id = 42, name = "LayerZero", token = "ZRO", value = BigDecimal("2500.00"), endsAt = endsAt)
            every { repo.findAllByIsActiveTrueOrderByCreatedAtDesc() } returns listOf(drop)

            val dto = service.getAllActive().first()

            assertEquals(42L, dto.id)
            assertEquals("LayerZero", dto.name)
            assertEquals("ZRO", dto.token)
            assertEquals(BigDecimal("2500.00"), dto.estimatedValue)
            assertEquals(endsAt, dto.endsAt)
            assertTrue(dto.isActive)
            assertEquals("https://example.com", dto.websiteUrl)
        }
    }

    // ── getHot ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHot()")
    inner class GetHot {

        @Test
        fun `returns airdrops in value-descending order from repo`() = runTest {
            val airdrops = listOf(
                airdrop(id = 1, value = BigDecimal("4200.00")),
                airdrop(id = 2, value = BigDecimal("2500.00")),
                airdrop(id = 3, value = BigDecimal("950.00"))
            )
            every { repo.findAllByIsActiveTrueOrderByEstimatedValueDesc() } returns airdrops

            val result = service.getHot()

            assertEquals(3, result.size)
            assertEquals(BigDecimal("4200.00"), result[0].estimatedValue)
            assertEquals(BigDecimal("2500.00"), result[1].estimatedValue)
            assertEquals(BigDecimal("950.00"), result[2].estimatedValue)
        }
    }

    // ── getStats ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStats()")
    inner class GetStats {

        @Test
        fun `returns correct aggregate values`() = runTest {
            every { repo.countByIsActiveTrue() } returns 10L
            every { repo.sumEstimatedValueActive() } returns BigDecimal("15000.00")
            every { repo.findEndingToday(any(), any()) } returns listOf(airdrop(), airdrop(id = 2))

            val stats = service.getStats()

            assertEquals(10L, stats.totalActive)
            assertEquals(BigDecimal("15000.00"), stats.totalEstimatedValue)
            assertEquals(2L, stats.endingToday)
        }

        @Test
        fun `returns zero when no active airdrops`() = runTest {
            every { repo.countByIsActiveTrue() } returns 0L
            every { repo.sumEstimatedValueActive() } returns BigDecimal.ZERO
            every { repo.findEndingToday(any(), any()) } returns emptyList()

            val stats = service.getStats()

            assertEquals(0L, stats.totalActive)
            assertEquals(BigDecimal.ZERO, stats.totalEstimatedValue)
            assertEquals(0L, stats.endingToday)
        }
    }

}
