package com.airdrophunter.dto

import com.airdrophunter.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("AirdropEntityDto")
class AirdropEntityDtoTest {

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    private fun entity(
        id: Long = 1L,
        name: String = "LayerZero",
        symbol: String = "ZRO",
        logoUrl: String? = "https://cdn.llama.fi/layerzero.png",
        minVal: BigDecimal = BigDecimal("1000.00"),
        maxVal: BigDecimal = BigDecimal("4000.00"),
        status: AirdropStatus = AirdropStatus.LIVE,
        category: AirdropCategory = AirdropCategory.BRIDGE,
        deadline: OffsetDateTime? = now.plusDays(12),
        steps: List<String> = listOf("Bridge ETH", "Swap on DEX", "Check eligibility"),
        isHot: Boolean = true,
        isPro: Boolean = false,
        slug: String? = "layerzero"
    ) = AirdropEntity(
        id = id, name = name, symbol = symbol, logoUrl = logoUrl,
        estimatedValueMin = minVal, estimatedValueMax = maxVal,
        status = status, category = category, deadline = deadline,
        steps = steps, isHot = isHot, isPro = isPro,
        llamaSlug = slug, lastRefreshedAt = now, createdAt = now
    )

    // ── from() factory ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("from() factory")
    inner class FromFactory {

        @Test
        fun `maps id correctly`() {
            assertEquals(42L, AirdropEntityDto.from(entity(id = 42L)).id)
        }

        @Test
        fun `maps name and symbol`() {
            val dto = AirdropEntityDto.from(entity(name = "zkSync Era", symbol = "ZK"))
            assertEquals("zkSync Era", dto.name)
            assertEquals("ZK", dto.symbol)
        }

        @Test
        fun `maps logoUrl including null`() {
            assertNull(AirdropEntityDto.from(entity(logoUrl = null)).logoUrl)
            assertEquals("https://cdn.llama.fi/layerzero.png",
                AirdropEntityDto.from(entity()).logoUrl)
        }

        @Test
        fun `maps estimated value min and max`() {
            val dto = AirdropEntityDto.from(entity(minVal = BigDecimal("500"), maxVal = BigDecimal("2000")))
            assertEquals(BigDecimal("500"), dto.estimatedValueMin)
            assertEquals(BigDecimal("2000"), dto.estimatedValueMax)
        }

        @Test
        fun `maps status enum`() {
            assertEquals(AirdropStatus.LIVE,  AirdropEntityDto.from(entity(status = AirdropStatus.LIVE)).status)
            assertEquals(AirdropStatus.SOON,  AirdropEntityDto.from(entity(status = AirdropStatus.SOON)).status)
            assertEquals(AirdropStatus.ENDED, AirdropEntityDto.from(entity(status = AirdropStatus.ENDED)).status)
        }

        @Test
        fun `maps category enum`() {
            AirdropCategory.values().forEach { cat ->
                assertEquals(cat, AirdropEntityDto.from(entity(category = cat)).category)
            }
        }

        @Test
        fun `maps deadline including null`() {
            assertNull(AirdropEntityDto.from(entity(deadline = null)).deadline)
            assertNotNull(AirdropEntityDto.from(entity()).deadline)
        }

        @Test
        fun `maps steps list preserving order`() {
            val steps = listOf("Step A", "Step B", "Step C")
            val dto = AirdropEntityDto.from(entity(steps = steps))
            assertEquals(steps, dto.steps)
        }

        @Test
        fun `maps empty steps list`() {
            val dto = AirdropEntityDto.from(entity(steps = emptyList()))
            assertTrue(dto.steps.isEmpty())
        }

        @Test
        fun `maps isHot and isPro flags`() {
            val hotPro = AirdropEntityDto.from(entity(isHot = true, isPro = true))
            assertTrue(hotPro.isHot)
            assertTrue(hotPro.isPro)

            val coldFree = AirdropEntityDto.from(entity(isHot = false, isPro = false))
            assertFalse(coldFree.isHot)
            assertFalse(coldFree.isPro)
        }

        @Test
        fun `maps lastRefreshedAt timestamp`() {
            val dto = AirdropEntityDto.from(entity())
            assertEquals(now, dto.lastRefreshedAt)
        }

        @Test
        fun `dto does not expose llamaSlug or createdAt`() {
            // Verify DTO fields by checking the data class toString doesn't expose internal fields
            val dto = AirdropEntityDto.from(entity(slug = "secret-slug"))
            val fields = dto.javaClass.declaredFields.map { it.name }
            assertFalse(fields.contains("llamaSlug"))
            assertFalse(fields.contains("createdAt"))
        }
    }

    // ── AirdropStatus enum ────────────────────────────────────────────────────

    @Nested
    @DisplayName("AirdropStatus enum")
    inner class StatusEnum {

        @Test
        fun `has exactly three values`() {
            assertEquals(3, AirdropStatus.values().size)
        }

        @Test
        fun `valueOf is case-exact`() {
            assertEquals(AirdropStatus.LIVE,  AirdropStatus.valueOf("LIVE"))
            assertEquals(AirdropStatus.SOON,  AirdropStatus.valueOf("SOON"))
            assertEquals(AirdropStatus.ENDED, AirdropStatus.valueOf("ENDED"))
        }

        @Test
        fun `ordinal order is LIVE, SOON, ENDED`() {
            assertEquals(0, AirdropStatus.LIVE.ordinal)
            assertEquals(1, AirdropStatus.SOON.ordinal)
            assertEquals(2, AirdropStatus.ENDED.ordinal)
        }
    }

    // ── AirdropCategory enum ──────────────────────────────────────────────────

    @Nested
    @DisplayName("AirdropCategory enum")
    inner class CategoryEnum {

        @Test
        fun `has exactly five values`() {
            assertEquals(5, AirdropCategory.values().size)
        }

        @Test
        fun `contains all expected categories`() {
            val names = AirdropCategory.values().map { it.name }.toSet()
            assertTrue(names.containsAll(setOf("L2", "DEFI", "BRIDGE", "AI", "OTHER")))
        }

        @Test
        fun `valueOf works for all categories`() {
            listOf("L2", "DEFI", "BRIDGE", "AI", "OTHER").forEach { name ->
                assertDoesNotThrow { AirdropCategory.valueOf(name) }
            }
        }
    }
}
