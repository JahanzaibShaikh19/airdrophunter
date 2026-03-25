package com.airdrophunter.repository

import com.airdrophunter.domain.Airdrop
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
@DisplayName("AirdropRepository")
class AirdropRepositoryTest {

    @Autowired lateinit var em: TestEntityManager
    @Autowired lateinit var repo: AirdropRepository

    private val now: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)

    private fun save(
        name: String = "Drop",
        value: BigDecimal = BigDecimal("100.00"),
        endsAt: OffsetDateTime = now.plusDays(5),
        isActive: Boolean = true
    ): Airdrop {
        val entity = Airdrop(
            name = name,
            description = "Desc",
            token = "TKN",
            protocol = "Proto",
            chain = "Ethereum",
            estimatedValue = value,
            endsAt = endsAt,
            isActive = isActive,
            websiteUrl = null,
            createdAt = now
        )
        return em.persistAndFlush(entity)
    }

    @BeforeEach
    fun clearDb() {
        repo.deleteAll()
        em.flush()
    }

    // ── findAllByIsActiveTrueOrderByCreatedAtDesc ────────────────────────────

    @Nested
    @DisplayName("findAllByIsActiveTrueOrderByCreatedAtDesc()")
    inner class FindAllActive {

        @Test
        fun `returns only active airdrops`() {
            save(name = "Active A", isActive = true)
            save(name = "Inactive", isActive = false)
            save(name = "Active B", isActive = true)

            val result = repo.findAllByIsActiveTrueOrderByCreatedAtDesc()

            assertEquals(2, result.size)
            assertTrue(result.all { it.isActive })
        }

        @Test
        fun `returns empty list when none active`() {
            save(isActive = false)
            save(isActive = false)

            val result = repo.findAllByIsActiveTrueOrderByCreatedAtDesc()

            assertTrue(result.isEmpty())
        }
    }

    // ── findAllByIsActiveTrueOrderByEstimatedValueDesc ───────────────────────

    @Nested
    @DisplayName("findAllByIsActiveTrueOrderByEstimatedValueDesc()")
    inner class FindHot {

        @Test
        fun `returns active airdrops sorted by value descending`() {
            save(name = "Low",  value = BigDecimal("100.00"), isActive = true)
            save(name = "High", value = BigDecimal("4200.00"), isActive = true)
            save(name = "Mid",  value = BigDecimal("2500.00"), isActive = true)

            val result = repo.findAllByIsActiveTrueOrderByEstimatedValueDesc()

            assertEquals(3, result.size)
            assertEquals("High", result[0].name)
            assertEquals("Mid",  result[1].name)
            assertEquals("Low",  result[2].name)
        }

        @Test
        fun `excludes inactive even if high value`() {
            save(name = "Ghost", value = BigDecimal("99999.00"), isActive = false)
            save(name = "Real",  value = BigDecimal("1000.00"),  isActive = true)

            val result = repo.findAllByIsActiveTrueOrderByEstimatedValueDesc()

            assertEquals(1, result.size)
            assertEquals("Real", result[0].name)
        }
    }

    // ── findEndingToday ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findEndingToday()")
    inner class FindEndingToday {

        @Test
        fun `returns airdrops ending within today window`() {
            val startOfDay = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC)
            val endOfDay = startOfDay.plusDays(1)

            save(name = "Ending",  endsAt = startOfDay.plusHours(3))
            save(name = "Future",  endsAt = endOfDay.plusHours(1))
            save(name = "Past",    endsAt = startOfDay.minusHours(1))
            save(name = "Inactive",endsAt = startOfDay.plusHours(2), isActive = false)

            val result = repo.findEndingToday(startOfDay, endOfDay)

            assertEquals(1, result.size)
            assertEquals("Ending", result[0].name)
        }

        @Test
        fun `returns empty when nothing ends today`() {
            val startOfDay = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC)
            val endOfDay = startOfDay.plusDays(1)

            save(endsAt = endOfDay.plusDays(5))

            val result = repo.findEndingToday(startOfDay, endOfDay)

            assertTrue(result.isEmpty())
        }
    }

    // ── countByIsActiveTrue ──────────────────────────────────────────────────

    @Nested
    @DisplayName("countByIsActiveTrue()")
    inner class CountActive {

        @Test
        fun `counts only active`() {
            save(isActive = true)
            save(isActive = true)
            save(isActive = false)

            assertEquals(2L, repo.countByIsActiveTrue())
        }

        @Test
        fun `returns zero when none active`() {
            save(isActive = false)
            assertEquals(0L, repo.countByIsActiveTrue())
        }
    }

    // ── sumEstimatedValueActive ──────────────────────────────────────────────

    @Nested
    @DisplayName("sumEstimatedValueActive()")
    inner class SumValue {

        @Test
        fun `sums values of active airdrops only`() {
            save(value = BigDecimal("1000.00"), isActive = true)
            save(value = BigDecimal("500.00"),  isActive = true)
            save(value = BigDecimal("9999.00"), isActive = false) // excluded

            val total = repo.sumEstimatedValueActive()

            assertEquals(0, BigDecimal("1500.00").compareTo(total))
        }

        @Test
        fun `returns zero when no active airdrops`() {
            save(isActive = false)

            val total = repo.sumEstimatedValueActive()

            assertEquals(0, BigDecimal.ZERO.compareTo(total))
        }
    }
}
