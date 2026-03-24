package com.airdrophunter.domain

import com.airdrophunter.domain.converter.StringListConverter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Pure unit tests for the [AirdropEntity] data class and [StringListConverter].
 * No Spring context needed.
 */
@DisplayName("AirdropEntity domain")
class AirdropEntityTest {

    private val now = OffsetDateTime.now(ZoneOffset.UTC)

    private fun makeEntity(
        id: Long = 1L,
        name: String = "EigenLayer",
        symbol: String = "EIGEN",
        logoUrl: String? = "https://cdn.llama.fi/eigenlayer.png",
        minVal: BigDecimal = BigDecimal("1500.00"),
        maxVal: BigDecimal = BigDecimal("5000.00"),
        status: AirdropStatus = AirdropStatus.LIVE,
        category: AirdropCategory = AirdropCategory.L2,
        deadline: OffsetDateTime? = now.plusDays(15),
        steps: List<String> = listOf("Stake ETH", "Delegate to AVS", "Check eligibility"),
        isHot: Boolean = true,
        isPro: Boolean = false,
        slug: String? = "eigenlayer"
    ) = AirdropEntity(
        id = id, name = name, symbol = symbol, logoUrl = logoUrl,
        estimatedValueMin = minVal, estimatedValueMax = maxVal,
        status = status, category = category, deadline = deadline,
        steps = steps, isHot = isHot, isPro = isPro,
        llamaSlug = slug, lastRefreshedAt = now, createdAt = now
    )

    // ── Data class contract ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Data class equality and copy")
    inner class DataClassContract {

        @Test
        fun `two entities with identical fields are equal`() {
            assertEquals(makeEntity(), makeEntity())
        }

        @Test
        fun `entities differing only by id are not equal`() {
            assertNotEquals(makeEntity(id = 1), makeEntity(id = 2))
        }

        @Test
        fun `copy preserves all fields by default`() {
            val original = makeEntity()
            val copy = original.copy()
            assertEquals(original, copy)
        }

        @Test
        fun `copy with name change produces distinct entity`() {
            val original = makeEntity(name = "Original")
            val modified = original.copy(name = "Modified")
            assertNotEquals(original, modified)
            assertEquals(original.id, modified.id)
            assertEquals(original.symbol, modified.symbol)
        }

        @Test
        fun `toString includes key fields`() {
            val entity = makeEntity(name = "Blast", symbol = "BLAST")
            val str = entity.toString()
            assertTrue(str.contains("Blast"))
            assertTrue(str.contains("BLAST"))
        }

        @Test
        fun `hashCode is consistent across equal instances`() {
            assertEquals(makeEntity().hashCode(), makeEntity().hashCode())
        }
    }

    // ── Field validation / defaults ────────────────────────────────────────────

    @Nested
    @DisplayName("Field defaults and nullable contracts")
    inner class FieldDefaults {

        @Test
        fun `id defaults to 0 for transient (unsaved) entity`() {
            val entity = AirdropEntity(
                name = "New", symbol = "NEW",
                estimatedValueMin = BigDecimal.ZERO, estimatedValueMax = BigDecimal.ZERO,
                status = AirdropStatus.LIVE, category = AirdropCategory.OTHER,
                steps = emptyList(), isHot = false, isPro = false,
                deadline = null, lastRefreshedAt = now
            )
            assertEquals(0L, entity.id)
        }

        @Test
        fun `steps list can be empty`() {
            val entity = makeEntity(steps = emptyList())
            assertTrue(entity.steps.isEmpty())
        }

        @Test
        fun `deadline can be null`() {
            val entity = makeEntity(deadline = null)
            assertNull(entity.deadline)
        }

        @Test
        fun `logoUrl can be null`() {
            val entity = makeEntity(logoUrl = null)
            assertNull(entity.logoUrl)
        }

        @Test
        fun `llamaSlug can be null`() {
            val entity = makeEntity(slug = null)
            assertNull(entity.llamaSlug)
        }

        @Test
        fun `estimatedValueMax can equal estimatedValueMin`() {
            val val1 = BigDecimal("1000.00")
            val entity = makeEntity(minVal = val1, maxVal = val1)
            assertEquals(entity.estimatedValueMin, entity.estimatedValueMax)
        }
    }

    // ── isHot / isPro logic ───────────────────────────────────────────────────

    @Nested
    @DisplayName("isHot and isPro flags")
    inner class Flags {

        @Test
        fun `isHot can be true and isPro false simultaneously`() {
            val entity = makeEntity(isHot = true, isPro = false)
            assertTrue(entity.isHot)
            assertFalse(entity.isPro)
        }

        @Test
        fun `both flags can be true at the same time`() {
            val entity = makeEntity(isHot = true, isPro = true)
            assertTrue(entity.isHot)
            assertTrue(entity.isPro)
        }

        @Test
        fun `both flags can be false at the same time`() {
            val entity = makeEntity(isHot = false, isPro = false)
            assertFalse(entity.isHot)
            assertFalse(entity.isPro)
        }
    }

    // ── Status and Category coverage ──────────────────────────────────────────

    @Nested
    @DisplayName("Status and Category coverage")
    inner class EnumAssignment {

        @Test
        fun `entity accepts all AirdropStatus values`() {
            AirdropStatus.values().forEach { status ->
                val entity = makeEntity(status = status)
                assertEquals(status, entity.status)
            }
        }

        @Test
        fun `entity accepts all AirdropCategory values`() {
            AirdropCategory.values().forEach { cat ->
                val entity = makeEntity(category = cat)
                assertEquals(cat, entity.category)
            }
        }
    }

    // ── StringListConverter ───────────────────────────────────────────────────

    @Nested
    @DisplayName("StringListConverter")
    inner class ConverterTests {

        private val converter = StringListConverter()

        @Test
        fun `converts single-element list`() {
            assertEquals("Only Step", converter.convertToDatabaseColumn(listOf("Only Step")))
        }

        @Test
        fun `converts multi-element list with pipe delimiter`() {
            val result = converter.convertToDatabaseColumn(listOf("A", "B", "C"))
            assertEquals("A|B|C", result)
        }

        @Test
        fun `converts empty list to empty string`() {
            assertEquals("", converter.convertToDatabaseColumn(emptyList()))
        }

        @Test
        fun `converts null list to empty string`() {
            assertEquals("", converter.convertToDatabaseColumn(null))
        }

        @Test
        fun `reads single element string`() {
            assertEquals(listOf("Only Step"), converter.convertToEntityAttribute("Only Step"))
        }

        @Test
        fun `reads pipe-delimited string back to list`() {
            assertEquals(listOf("A", "B", "C"), converter.convertToEntityAttribute("A|B|C"))
        }

        @Test
        fun `reads null as empty list`() {
            assertEquals(emptyList<String>(), converter.convertToEntityAttribute(null))
        }

        @Test
        fun `reads blank string as empty list`() {
            assertEquals(emptyList<String>(), converter.convertToEntityAttribute("   "))
        }

        @Test
        fun `trims whitespace from each element on read`() {
            val result = converter.convertToEntityAttribute("  Step 1  |  Step 2  ")
            assertEquals(listOf("Step 1", "Step 2"), result)
        }

        @Test
        fun `round-trip is lossless for typical steps list`() {
            val original = listOf(
                "Bridge ETH to the L2 network",
                "Perform at least 5 transactions",
                "Check eligibility on the portal"
            )
            val db = converter.convertToDatabaseColumn(original)
            val roundTripped = converter.convertToEntityAttribute(db)

            assertEquals(original, roundTripped)
        }

        @Test
        fun `filters out empty elements produced by trailing pipes`() {
            // Malformed DB data edge case
            val result = converter.convertToEntityAttribute("A||B|")
            assertEquals(listOf("A", "B"), result)
        }
    }
}
