package com.airdrophunter.service

import com.airdrophunter.client.LlamaProtocol
import com.airdrophunter.domain.*
import com.airdrophunter.repository.AirdropEntityRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Optional

/**
 * Unit tests for [DefiLlamaService].
 *
 * WebClient is NOT called in these tests — we mock [AirdropEntityRepository]
 * and call the non-HTTP methods directly.
 */
class DefiLlamaServiceTest {

    private val repo: AirdropEntityRepository = mockk(relaxed = true)
    private val webClient: WebClient = mockk(relaxed = true)
    private lateinit var service: DefiLlamaService

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun proto(
        name: String = "TestProto",
        symbol: String = "TST",
        slug: String = "test-proto",
        category: String? = null,
        tvl: Double? = 50_000_000.0,
        listedAt: Long? = null,
        airdrop: Boolean? = null
    ) = LlamaProtocol(
        id = slug, name = name, symbol = symbol, slug = slug,
        category = category, tvl = tvl, listedAt = listedAt, airdrop = airdrop
    )

    private fun entity(
        id: Long = 1L,
        slug: String = "test",
        status: AirdropStatus = AirdropStatus.LIVE,
        isHot: Boolean = false,
        maxVal: BigDecimal = BigDecimal("1000")
    ) = AirdropEntity(
        id = id, name = "Test", symbol = "TST", llamaSlug = slug,
        estimatedValueMin = BigDecimal("100"), estimatedValueMax = maxVal,
        status = status, category = AirdropCategory.OTHER,
        steps = listOf("Step 1", "Step 2"),
        isHot = isHot, isPro = false,
        deadline = OffsetDateTime.now(ZoneOffset.UTC).plusDays(30),
        lastRefreshedAt = OffsetDateTime.now(ZoneOffset.UTC)
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = DefiLlamaService(webClient, repo)
    }

    // ── upsertAll ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("upsertAll()")
    inner class UpsertAll {

        @Test
        fun `saves new entity when slug does not exist`() {
            val newEntity = entity(id = 0, slug = "new-slug")
            every { repo.findByLlamaSlug("new-slug") } returns Optional.empty()
            every { repo.save(any()) } returns newEntity

            service.upsertAll(listOf(newEntity))

            verify(exactly = 1) { repo.save(any()) }
        }

        @Test
        fun `updates existing entity preserving original createdAt`() {
            val existing = entity(id = 99, slug = "existing-slug")
            val incoming = entity(id = 0, slug = "existing-slug")
            every { repo.findByLlamaSlug("existing-slug") } returns Optional.of(existing)
            val savedSlot = slot<AirdropEntity>()
            every { repo.save(capture(savedSlot)) } returns existing

            service.upsertAll(listOf(incoming))

            verify(exactly = 1) { repo.save(any()) }
            assertEquals(99L, savedSlot.captured.id)
            assertEquals(existing.createdAt, savedSlot.captured.createdAt)
        }

        @Test
        fun `saves entity with null slug without checking findByLlamaSlug`() {
            val noSlug = entity(slug = "null-slug").copy(llamaSlug = null)
            every { repo.save(any()) } returns noSlug

            service.upsertAll(listOf(noSlug))

            verify(exactly = 0) { repo.findByLlamaSlug(any()) }
            verify(exactly = 1) { repo.save(any()) }
        }

        @Test
        fun `handles empty list gracefully`() {
            service.upsertAll(emptyList())
            verify(exactly = 0) { repo.save(any()) }
        }
    }

    // ── Query helpers ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Query helpers")
    inner class QueryHelpers {

        @Test
        fun `getLiveAirdrops delegates to repo`() {
            every { repo.findAllByStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE) } returns listOf(entity())
            val result = service.getLiveAirdrops()
            assertEquals(1, result.size)
        }

        @Test
        fun `getHotAirdrops returns only hot entities`() {
            every { repo.findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE) } returns
                    listOf(entity(isHot = true))
            val result = service.getHotAirdrops()
            assertTrue(result.all { it.isHot })
        }

        @Test
        fun `getByCategory passes correct category to repo`() {
            every {
                repo.findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE, AirdropCategory.L2)
            } returns listOf(entity())

            val result = service.getByCategory(AirdropCategory.L2)
            assertEquals(1, result.size)
            verify {
                repo.findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE, AirdropCategory.L2)
            }
        }

        @Test
        fun `getEndingSoon passes threshold to repo`() {
            every { repo.findEndingSoon(any(), any()) } returns emptyList()
            service.getEndingSoon(48)
            verify { repo.findEndingSoon(AirdropStatus.LIVE, any()) }
        }
    }

    // ── applyFallbackData ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyFallbackData()")
    inner class FallbackData {

        @Test
        fun `writes mock data when DB is empty`() {
            every { repo.countLive() } returns 0L
            every { repo.findByLlamaSlug(any()) } returns Optional.empty()
            every { repo.save(any()) } answers { firstArg() }

            service.applyFallbackData()

            verify(atLeast = 1) { repo.save(any()) }
        }

        @Test
        fun `skips writing when live data already exists`() {
            every { repo.countLive() } returns 5L

            service.applyFallbackData()

            verify(exactly = 0) { repo.save(any()) }
        }

        @Test
        fun `does not insert duplicate fallback slugs`() {
            every { repo.countLive() } returns 0L
            // Simulate first mock already present
            every { repo.findByLlamaSlug(eq("layerzero-fallback")) } returns Optional.of(entity())
            every { repo.findByLlamaSlug(neq("layerzero-fallback")) } returns Optional.empty()
            every { repo.save(any()) } answers { firstArg() }

            service.applyFallbackData()

            // LayerZero should NOT be saved; others should
            verify(exactly = 0) { repo.save(match { it.llamaSlug == "layerzero-fallback" }) }
        }

        @Test
        fun `all fallback entities have non-empty steps`() {
            every { repo.countLive() } returns 0L
            every { repo.findByLlamaSlug(any()) } returns Optional.empty()
            val savedEntities = mutableListOf<AirdropEntity>()
            every { repo.save(capture(savedEntities)) } answers { firstArg() }

            service.applyFallbackData()

            savedEntities.forEach { entity ->
                assertTrue(entity.steps.isNotEmpty(), "${entity.name} must have steps")
            }
        }

        @Test
        fun `all fallback entities have positive max value`() {
            every { repo.countLive() } returns 0L
            every { repo.findByLlamaSlug(any()) } returns Optional.empty()
            val savedEntities = mutableListOf<AirdropEntity>()
            every { repo.save(capture(savedEntities)) } answers { firstArg() }

            service.applyFallbackData()

            savedEntities.forEach { entity ->
                assertTrue(entity.estimatedValueMax > BigDecimal.ZERO,
                    "${entity.name} must have positive max value")
            }
        }
    }

    // ── StringListConverter integration ──────────────────────────────────────

    @Nested
    @DisplayName("StringListConverter")
    inner class ConverterTest {

        private val converter = com.airdrophunter.domain.converter.StringListConverter()

        @Test
        fun `converts list to pipe-delimited string`() {
            val result = converter.convertToDatabaseColumn(listOf("Step 1", "Step 2", "Step 3"))
            assertEquals("Step 1|Step 2|Step 3", result)
        }

        @Test
        fun `converts pipe-delimited string back to list`() {
            val result = converter.convertToEntityAttribute("Step 1|Step 2|Step 3")
            assertEquals(listOf("Step 1", "Step 2", "Step 3"), result)
        }

        @Test
        fun `converts null/blank to empty list`() {
            assertEquals(emptyList<String>(), converter.convertToEntityAttribute(null))
            assertEquals(emptyList<String>(), converter.convertToEntityAttribute(""))
            assertEquals(emptyList<String>(), converter.convertToEntityAttribute("   "))
        }

        @Test
        fun `converts null/empty list to empty string`() {
            assertEquals("", converter.convertToDatabaseColumn(null))
            assertEquals("", converter.convertToDatabaseColumn(emptyList()))
        }

        @Test
        fun `trims whitespace from each element during read`() {
            val result = converter.convertToEntityAttribute("  Step A  |  Step B  ")
            assertEquals(listOf("Step A", "Step B"), result)
        }
    }
}
