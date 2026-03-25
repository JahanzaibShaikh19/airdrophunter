package com.airdrophunter.service

import com.airdrophunter.domain.Airdrop
import com.airdrophunter.dto.AirdropDto
import com.airdrophunter.dto.StatsDto
import com.airdrophunter.repository.AirdropRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class AirdropService(private val repo: AirdropRepository) {

    private val log = LoggerFactory.getLogger(AirdropService::class.java)

    // ── Mapping ─────────────────────────────────────────────────────────────

    private fun Airdrop.toDto() = AirdropDto(
        id = id,
        name = name,
        description = description,
        token = token,
        protocol = protocol,
        chain = chain,
        estimatedValue = estimatedValue,
        endsAt = endsAt,
        isActive = isActive,
        websiteUrl = websiteUrl,
        createdAt = createdAt
    )

    // ── Queries ──────────────────────────────────────────────────────────────

    suspend fun getAllActive(): List<AirdropDto> = withContext(Dispatchers.IO) {
        log.debug("Fetching all active airdrops")
        repo.findAllByIsActiveTrueOrderByCreatedAtDesc().map { it.toDto() }
    }

    suspend fun getHot(): List<AirdropDto> = withContext(Dispatchers.IO) {
        log.debug("Fetching hot airdrops sorted by value")
        repo.findAllByIsActiveTrueOrderByEstimatedValueDesc().map { it.toDto() }
    }

    suspend fun getStats(): StatsDto = withContext(Dispatchers.IO) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val startOfDay = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC)
        val endOfDay = startOfDay.plusDays(1)

        val total = repo.countByIsActiveTrue()
        val totalValue = repo.sumEstimatedValueActive()
        val endingToday = repo.findEndingToday(startOfDay, endOfDay).size.toLong()

        StatsDto(
            totalActive = total,
            totalEstimatedValue = totalValue,
            endingToday = endingToday
        )
    }
}
