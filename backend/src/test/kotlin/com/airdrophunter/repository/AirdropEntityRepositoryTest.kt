package com.airdrophunter.repository

import com.airdrophunter.domain.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AirdropEntityRepository")
class AirdropEntityRepositoryTest {

    @Autowired lateinit var em: TestEntityManager
    @Autowired lateinit var repo: AirdropEntityRepository

    private val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

    // ── Fixture ──────────────────────────────────────────────────────────────

    private fun save(
        name: String = "TestDrop",
        symbol: String = "TKN",
        status: AirdropStatus = AirdropStatus.LIVE,
        category: AirdropCategory = AirdropCategory.OTHER,
        maxVal: BigDecimal = BigDecimal("1000.00"),
        minVal: BigDecimal = BigDecimal("200.00"),
        isHot: Boolean = false,
        isPro: Boolean = false,
        deadline: OffsetDateTime? = now.plusDays(30),
        slug: String? = name.lowercase().replace(" ", "-")
    ): AirdropEntity {
        val entity = AirdropEntity(
            name = name, symbol = symbol,
            estimatedValueMin = minVal, estimatedValueMax = maxVal,
            status = status, category = category,
            steps = listOf("Step 1", "Step 2"),
            isHot = isHot, isPro = isPro,
            deadline = deadline, llamaSlug = slug,
            lastRefreshedAt = now, createdAt = now
        )
        return em.persistAndFlush(entity)
    }

    @BeforeEach
    fun clear() { repo.deleteAll(); em.flush() }

    // ── findAllByStatusOrderByEstimatedValueMaxDesc ───────────────────────────

    @Nested
    @DisplayName("findAllByStatusOrderByEstimatedValueMaxDesc()")
    inner class FindByStatus {

        @Test
        fun `returns only LIVE entities sorted by max value descending`() {
            save("Low",  maxVal = BigDecimal("500"),  status = AirdropStatus.LIVE)
            save("High", maxVal = BigDecimal("4000"), status = AirdropStatus.LIVE)
            save("Mid",  maxVal = BigDecimal("2000"), status = AirdropStatus.LIVE)
            save("Soon", maxVal = BigDecimal("9999"), status = AirdropStatus.SOON)  // excluded

            val result = repo.findAllByStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE)

            assertEquals(3, result.size)
            assertEquals("High", result[0].name)
            assertEquals("Mid",  result[1].name)
            assertEquals("Low",  result[2].name)
        }

        @Test
        fun `returns empty list when no entities match status`() {
            save(status = AirdropStatus.ENDED)
            val result = repo.findAllByStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE)
            assertTrue(result.isEmpty())
        }
    }

    // ── findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc ────────────────

    @Nested
    @DisplayName("findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc()")
    inner class FindByStatusAndCategory {

        @Test
        fun `returns only matching status AND category`() {
            save("L2Drop",    category = AirdropCategory.L2,    status = AirdropStatus.LIVE, maxVal = BigDecimal("3000"))
            save("BridgeDrop",category = AirdropCategory.BRIDGE,status = AirdropStatus.LIVE, maxVal = BigDecimal("2000"))
            save("L2Soon",    category = AirdropCategory.L2,    status = AirdropStatus.SOON) // wrong status

            val result = repo.findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc(
                AirdropStatus.LIVE, AirdropCategory.L2
            )

            assertEquals(1, result.size)
            assertEquals("L2Drop", result[0].name)
            assertEquals(AirdropCategory.L2, result[0].category)
        }
    }

    // ── findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc ───────────────

    @Nested
    @DisplayName("findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc()")
    inner class FindHot {

        @Test
        fun `returns only hot LIVE entities sorted by max value desc`() {
            save("HotA", isHot = true,  maxVal = BigDecimal("5000"), status = AirdropStatus.LIVE)
            save("HotB", isHot = true,  maxVal = BigDecimal("3000"), status = AirdropStatus.LIVE)
            save("Cold", isHot = false, maxVal = BigDecimal("9999"), status = AirdropStatus.LIVE)
            save("HotEnded", isHot = true, status = AirdropStatus.ENDED)

            val result = repo.findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE)

            assertEquals(2, result.size)
            assertTrue(result.all { it.isHot })
            assertEquals("HotA", result[0].name)
            assertEquals("HotB", result[1].name)
        }

        @Test
        fun `returns empty when no hot live entities`() {
            save("Cold", isHot = false)
            val result = repo.findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE)
            assertTrue(result.isEmpty())
        }
    }

    // ── findByLlamaSlug ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByLlamaSlug()")
    inner class FindBySlug {

        @Test
        fun `returns entity when slug matches`() {
            save(name = "LayerZero", slug = "layerzero")

            val result = repo.findByLlamaSlug("layerzero")

            assertTrue(result.isPresent)
            assertEquals("LayerZero", result.get().name)
        }

        @Test
        fun `returns empty Optional when slug not found`() {
            val result = repo.findByLlamaSlug("nonexistent-slug")
            assertTrue(result.isEmpty)
        }

        @Test
        fun `slug is case-sensitive`() {
            save(slug = "layerzero")
            assertTrue(repo.findByLlamaSlug("LayerZero").isEmpty)
        }
    }

    // ── findEndingSoon ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findEndingSoon()")
    inner class FindEndingSoon {

        @Test
        fun `returns LIVE entities whose deadline is before threshold`() {
            val threshold = now.plusHours(72)

            save("EndsSoon",  deadline = now.plusHours(24), status = AirdropStatus.LIVE)
            save("EndsFuture",deadline = now.plusDays(30),  status = AirdropStatus.LIVE)
            save("SoonEnded", deadline = now.plusHours(12), status = AirdropStatus.ENDED) // wrong status

            val result = repo.findEndingSoon(AirdropStatus.LIVE, threshold)

            assertEquals(1, result.size)
            assertEquals("EndsSoon", result[0].name)
        }

        @Test
        fun `returns entities ordered by deadline ascending`() {
            val threshold = now.plusHours(100)

            save("SoonA", deadline = now.plusHours(10), status = AirdropStatus.LIVE, slug = "a")
            save("SoonB", deadline = now.plusHours(5),  status = AirdropStatus.LIVE, slug = "b")
            save("SoonC", deadline = now.plusHours(20), status = AirdropStatus.LIVE, slug = "c")

            val result = repo.findEndingSoon(AirdropStatus.LIVE, threshold)

            assertEquals(3, result.size)
            assertEquals("SoonB", result[0].name)  // 5h — earliest
            assertEquals("SoonA", result[1].name)  // 10h
            assertEquals("SoonC", result[2].name)  // 20h
        }

        @Test
        fun `excludes entities with null deadline`() {
            val threshold = now.plusDays(30)
            save("NullDeadline", deadline = null)

            val result = repo.findEndingSoon(AirdropStatus.LIVE, threshold)

            assertTrue(result.isEmpty())
        }
    }

    // ── countLive ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("countLive()")
    inner class CountLive {

        @Test
        fun `counts only LIVE entities`() {
            save(status = AirdropStatus.LIVE, slug = "a")
            save(status = AirdropStatus.LIVE, slug = "b")
            save(status = AirdropStatus.SOON, slug = "c")
            save(status = AirdropStatus.ENDED, slug = "d")

            assertEquals(2L, repo.countLive())
        }

        @Test
        fun `returns 0 when no live entities exist`() {
            save(status = AirdropStatus.ENDED)
            assertEquals(0L, repo.countLive())
        }
    }

    // ── sumMaxValueLive ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("sumMaxValueLive()")
    inner class SumMaxValue {

        @Test
        fun `sums estimatedValueMax of LIVE entities only`() {
            save(maxVal = BigDecimal("2000.00"), status = AirdropStatus.LIVE, slug = "a")
            save(maxVal = BigDecimal("1000.00"), status = AirdropStatus.LIVE, slug = "b")
            save(maxVal = BigDecimal("9999.00"), status = AirdropStatus.SOON, slug = "c") // excluded

            val total = repo.sumMaxValueLive()

            assertEquals(0, BigDecimal("3000.00").compareTo(total))
        }

        @Test
        fun `returns zero when no live entities`() {
            save(status = AirdropStatus.ENDED)
            assertEquals(0, BigDecimal.ZERO.compareTo(repo.sumMaxValueLive()))
        }
    }

    // ── markExpiredAsEnded ────────────────────────────────────────────────────

    @Nested
    @DisplayName("markExpiredAsEnded()")
    inner class MarkExpired {

        @Test
        fun `marks LIVE entities with past deadline as ENDED`() {
            save("PastDeadline",   deadline = now.minusHours(1), status = AirdropStatus.LIVE,  slug = "past")
            save("FutureDeadline", deadline = now.plusDays(5),   status = AirdropStatus.LIVE,  slug = "future")
            save("AlreadyEnded",   deadline = now.minusHours(5), status = AirdropStatus.ENDED, slug = "done")

            val updated = repo.markExpiredAsEnded(now)
            em.flush(); em.clear()

            assertEquals(1, updated)
            assertEquals(AirdropStatus.ENDED, repo.findByLlamaSlug("past").get().status)
            assertEquals(AirdropStatus.LIVE,  repo.findByLlamaSlug("future").get().status)
            assertEquals(AirdropStatus.ENDED, repo.findByLlamaSlug("done").get().status)
        }

        @Test
        fun `returns 0 when nothing needs to expire`() {
            save(deadline = now.plusDays(10), slug = "far-future")
            val updated = repo.markExpiredAsEnded(now)
            assertEquals(0, updated)
        }

        @Test
        fun `does not expire SOON entities even if past deadline`() {
            save(deadline = now.minusHours(1), status = AirdropStatus.SOON)
            // SOON can only be updated to ENDED by explicit logic, not this query
            val updated = repo.markExpiredAsEnded(now)
            assertEquals(0, updated)
        }
    }
}
