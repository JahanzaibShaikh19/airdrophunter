package com.airdrophunter.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("DTO data classes")
class DtoTest {

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    @Test
    fun `AirdropDto equality holds for same values`() {
        val a = AirdropDto(
            id = 1L, name = "Test", description = "Desc", token = "TST",
            protocol = "P", chain = "ETH", estimatedValue = BigDecimal("100"),
            endsAt = now, isActive = true, websiteUrl = null, createdAt = now
        )
        val b = a.copy()
        assertEquals(a, b)
    }

    @Test
    fun `AirdropDto copy produces distinct object with same data`() {
        val original = AirdropDto(
            id = 5L, name = "Original", description = "D", token = "OG",
            protocol = "P", chain = "Cosmos", estimatedValue = BigDecimal("999"),
            endsAt = now, isActive = false, websiteUrl = "https://test.com", createdAt = now
        )
        val copy = original.copy(name = "Copy")
        assertNotEquals(original, copy)
        assertEquals("Copy", copy.name)
        assertEquals(5L, copy.id)
    }

    @Test
    fun `WalletCheckRequest stores address as-is`() {
        val req = WalletCheckRequest("0xabc")
        assertEquals("0xabc", req.address)
    }

    @Test
    fun `WalletCheckResponse eligible=true carries reward and airdrops`() {
        val res = WalletCheckResponse(
            address = "0xabc",
            eligible = true,
            reason = "ok",
            estimatedReward = BigDecimal("275.00"),
            eligibleAirdrops = listOf("LayerZero", "zkSync")
        )
        assertTrue(res.eligible)
        assertEquals(2, res.eligibleAirdrops.size)
        assertEquals(BigDecimal("275.00"), res.estimatedReward)
    }

    @Test
    fun `WalletCheckResponse eligible=false has zero reward and empty list`() {
        val res = WalletCheckResponse(
            address = "0xbad",
            eligible = false,
            reason = "not eligible",
            estimatedReward = BigDecimal.ZERO,
            eligibleAirdrops = emptyList()
        )
        assertFalse(res.eligible)
        assertEquals(BigDecimal.ZERO, res.estimatedReward)
        assertTrue(res.eligibleAirdrops.isEmpty())
    }

    @Test
    fun `StatsDto holds correct aggregate values`() {
        val stats = StatsDto(
            totalActive = 10L,
            totalEstimatedValue = BigDecimal("16340.00"),
            endingToday = 2L
        )
        assertEquals(10L, stats.totalActive)
        assertEquals(BigDecimal("16340.00"), stats.totalEstimatedValue)
        assertEquals(2L, stats.endingToday)
    }

    @Test
    fun `StatsDto zero state is valid`() {
        val stats = StatsDto(
            totalActive = 0L,
            totalEstimatedValue = BigDecimal.ZERO,
            endingToday = 0L
        )
        assertEquals(0L, stats.totalActive)
        assertEquals(BigDecimal.ZERO, stats.totalEstimatedValue)
        assertEquals(0L, stats.endingToday)
    }
}
