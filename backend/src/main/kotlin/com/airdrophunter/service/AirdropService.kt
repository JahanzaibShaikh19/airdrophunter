package com.airdrophunter.service

import com.airdrophunter.domain.Airdrop
import com.airdrophunter.dto.AirdropDto
import com.airdrophunter.dto.StatsDto
import com.airdrophunter.dto.WalletCheckRequest
import com.airdrophunter.dto.WalletCheckResponse
import com.airdrophunter.repository.AirdropRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    // ── Wallet Check (async eligibility simulation) ──────────────────────────

    suspend fun checkWallet(request: WalletCheckRequest): WalletCheckResponse =
        withContext(Dispatchers.IO) {
            log.debug("Checking wallet eligibility for: ${request.address}")

            // Simulate on-chain lookup delay (would be real RPC call in production)
            delay(300)

            val address = request.address.trim().lowercase()
            val isValidEthAddress = address.matches(Regex("^0x[0-9a-f]{40}$"))
            val isSolanaAddress = address.length in 32..44 && !address.startsWith("0x")

            if (!isValidEthAddress && !isSolanaAddress) {
                return@withContext WalletCheckResponse(
                    address = request.address,
                    eligible = false,
                    reason = "Invalid wallet address format. Please provide a valid EVM (0x...) or Solana address.",
                    estimatedReward = BigDecimal.ZERO,
                    eligibleAirdrops = emptyList()
                )
            }

            // Deterministic eligibility based on address hash (real impl would call RPC nodes)
            val hash = address.hashCode()
            val eligible = hash % 3 != 0   // ~66% chance eligible for demo

            val activeAirdrops = repo.findAllByIsActiveTrueOrderByEstimatedValueDesc()

            return@withContext if (eligible) {
                val qualifiedAirdrops = activeAirdrops
                    .filter { (it.name.hashCode() + hash) % 2 == 0 }
                    .take(4)

                val reward = qualifiedAirdrops
                    .fold(BigDecimal.ZERO) { acc, a -> acc + a.estimatedValue.multiply(BigDecimal("0.05")) }

                WalletCheckResponse(
                    address = request.address,
                    eligible = true,
                    reason = "Wallet is eligible based on on-chain activity and protocol interaction history.",
                    estimatedReward = reward,
                    eligibleAirdrops = qualifiedAirdrops.map { it.name }
                )
            } else {
                WalletCheckResponse(
                    address = request.address,
                    eligible = false,
                    reason = "Wallet does not meet eligibility criteria. Try participating in more DeFi protocols.",
                    estimatedReward = BigDecimal.ZERO,
                    eligibleAirdrops = emptyList()
                )
            }
        }
}
