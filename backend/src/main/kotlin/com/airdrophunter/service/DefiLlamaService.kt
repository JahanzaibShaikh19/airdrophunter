package com.airdrophunter.service

import com.airdrophunter.client.LlamaProtocol
import com.airdrophunter.client.LlamaPriceResponse
import com.airdrophunter.domain.*
import com.airdrophunter.repository.AirdropEntityRepository
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/**
 * Fetches live airdrop data from DeFiLlama APIs, transforms it into
 * [AirdropEntity] records, and persists them to PostgreSQL.
 *
 * Refresh cycle: every 15 minutes via [@Scheduled].
 * Fallback: if the API is unreachable, hard-coded mock data is written
 * once on startup and kept until the next successful refresh.
 */
@Service
class DefiLlamaService(
    private val webClient: WebClient,
    private val repo: AirdropEntityRepository,
    private val telegramAlertService: TelegramAlertService
) {
    private val log = LoggerFactory.getLogger(DefiLlamaService::class.java)

    // ── Constants ────────────────────────────────────────────────────────────

    private val PROTOCOLS_URL = "https://api.llama.fi/protocols"
    private val PRICES_URL    = "https://coins.llama.fi/prices/current/"

    /** Protocols that are confirmed or strongly expected airdrop candidates */
    private val AIRDROP_KEYWORDS = setOf(
        "zk", "layer", "eigen", "scroll", "linea", "blast", "taiko",
        "hyperliquid", "wormhole", "celestia", "saga", "dymension",
        "berachain", "monad", "movement", "fuel", "eclipse"
    )

    private val BRIDGE_KEYWORDS  = setOf("bridge", "portal", "wormhole", "hop", "across", "stargate")
    private val AI_KEYWORDS      = setOf("ai", "gpt", "neural", "bittensor", "fetch", "ocean")
    private val L2_KEYWORDS      = setOf("l2", "rollup", "zkevm", "optimism", "arbitrum", "starkware", "zksync")

    // ── Steps templates by category ──────────────────────────────────────────

    private val L2_STEPS = listOf(
        "Bridge ETH to the L2 network using the official bridge",
        "Perform at least 5 transactions on-chain",
        "Interact with 2+ native DeFi protocols (swap, lend, LP)",
        "Hold a balance over multiple weeks",
        "Check eligibility on the official airdrop portal"
    )
    private val BRIDGE_STEPS = listOf(
        "Bridge assets across at least 3 different chains",
        "Complete 10+ bridge transactions",
        "Use the protocol during incentivised campaign periods",
        "Hold the protocol's governance token (if applicable)",
        "Check eligibility on the official airdrop portal"
    )
    private val DEFI_STEPS = listOf(
        "Supply liquidity to a qualifying pool",
        "Borrow against your collateral at least once",
        "Trade or swap $500+ volume on the DEX",
        "Vote on at least one governance proposal",
        "Check eligibility on the official airdrop portal"
    )
    private val AI_STEPS = listOf(
        "Register and verify your wallet on the platform",
        "Complete on-boarding tasks or quizzes",
        "Stake or delegate tokens to a validator",
        "Participate in community governance",
        "Check eligibility on the official airdrop portal"
    )
    private val DEFAULT_STEPS = listOf(
        "Connect your wallet to the protocol",
        "Perform protocol-specific interactions",
        "Meet minimum activity thresholds",
        "Check eligibility on the official airdrop portal"
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Trigger a first fetch immediately on startup */
    @PostConstruct
    fun init() {
        log.info("DefiLlamaService initialising — triggering first refresh")
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            refreshAirdrops()
        }
    }

    // ── Scheduler ────────────────────────────────────────────────────────────

    /**
     * Runs every 15 minutes (900 000 ms).
     * Marks expired rows first, then fetches fresh data.
     */
    @Scheduled(fixedDelay = 900_000, initialDelay = 900_000)
    fun scheduledRefresh() {
        log.info("Scheduled DeFiLlama refresh triggered")
        runBlocking(Dispatchers.IO) {
            refreshAirdrops()
        }
    }

    // ── Core refresh logic ────────────────────────────────────────────────────

    suspend fun refreshAirdrops() {
        try {
            log.info("Fetching protocols from DeFiLlama…")
            val protocols = fetchProtocols()
            log.info("Received ${protocols.size} protocols from DeFiLlama")

            val candidates = filterAirdropCandidates(protocols)
            log.info("Identified ${candidates.size} airdrop candidates")

            val priceMap = fetchPrices(candidates)

            val entities = candidates.map { proto ->
                buildEntity(proto, priceMap)
            }

            upsertAll(entities)

            // Mark any rows whose deadline has passed
            val expired = repo.markExpiredAsEnded(OffsetDateTime.now(ZoneOffset.UTC))
            if (expired > 0) log.info("Marked $expired airdrop(s) as ENDED")

            log.info("DeFiLlama refresh complete — ${entities.size} records upserted")
        } catch (ex: Exception) {
            log.error("DeFiLlama API call failed: ${ex.message}. Falling back to mock data.", ex)
            applyFallbackData()
        }
    }

    // ── HTTP fetchers ─────────────────────────────────────────────────────────

    private suspend fun fetchProtocols(): List<LlamaProtocol> =
        webClient.get()
            .uri(PROTOCOLS_URL)
            .retrieve()
            .awaitBody<List<LlamaProtocol>>()

    private suspend fun fetchPrices(protocols: List<LlamaProtocol>): Map<String, Double> {
        if (protocols.isEmpty()) return emptyMap()

        val coinIds = protocols
            .mapNotNull { p -> p.symbol?.let { "coingecko:${it.lowercase()}" } }
            .take(100)                      // API limit
            .joinToString(",")

        return try {
            val response = webClient.get()
                .uri("$PRICES_URL$coinIds")
                .retrieve()
                .awaitBody<LlamaPriceResponse>()
            response.coins.mapValues { (_, v) -> v.price ?: 0.0 }
        } catch (ex: Exception) {
            log.warn("Price fetch failed (${ex.message}) — using TVL-based estimates")
            emptyMap()
        }
    }

    // ── Transformation ────────────────────────────────────────────────────────

    /**
     * Filters the full protocol list down to plausible airdrop candidates by:
     * 1. Matching name/slug against known airdrop keywords
     * 2. Checking the explicit [LlamaProtocol.airdrop] flag
     * 3. Keeping only protocols with a known symbol (excludes scams/forks)
     */
    private fun filterAirdropCandidates(protocols: List<LlamaProtocol>): List<LlamaProtocol> =
        protocols.filter { proto ->
            val lowerName = proto.name.lowercase()
            val lowerSlug = proto.slug?.lowercase() ?: ""
            val hasKeyword = AIRDROP_KEYWORDS.any { kw -> lowerName.contains(kw) || lowerSlug.contains(kw) }
            val explicitFlag = proto.airdrop == true
            val hasSymbol = !proto.symbol.isNullOrBlank()
            (hasKeyword || explicitFlag) && hasSymbol
        }.take(50)                          // cap DB writes per refresh

    private fun resolveCategory(proto: LlamaProtocol): AirdropCategory {
        val lower = "${proto.name} ${proto.category ?: ""}".lowercase()
        return when {
            L2_KEYWORDS.any     { lower.contains(it) } -> AirdropCategory.L2
            BRIDGE_KEYWORDS.any { lower.contains(it) } -> AirdropCategory.BRIDGE
            AI_KEYWORDS.any     { lower.contains(it) } -> AirdropCategory.AI
            proto.category?.lowercase() == "dexes"      -> AirdropCategory.DEFI
            proto.category?.lowercase() == "lending"    -> AirdropCategory.DEFI
            proto.category?.lowercase() == "yield"      -> AirdropCategory.DEFI
            else -> AirdropCategory.OTHER
        }
    }

    private fun resolveSteps(category: AirdropCategory): List<String> = when (category) {
        AirdropCategory.L2     -> L2_STEPS
        AirdropCategory.BRIDGE -> BRIDGE_STEPS
        AirdropCategory.DEFI   -> DEFI_STEPS
        AirdropCategory.AI     -> AI_STEPS
        else                   -> DEFAULT_STEPS
    }

    /**
     * Estimates value range from either:
     * - Live token price (from coins.llama.fi) × an assumed 1 000-token grant
     * - TVL-derivative bucket if no price is available
     */
    private fun estimateValueRange(
        proto: LlamaProtocol,
        priceMap: Map<String, Double>
    ): Pair<BigDecimal, BigDecimal> {
        val priceKey = "coingecko:${proto.symbol?.lowercase()}"
        val price = priceMap[priceKey]

        return if (price != null && price > 0) {
            val assumedGrant = 1_000.0
            val base = BigDecimal(price * assumedGrant).setScale(2, RoundingMode.HALF_UP)
            base to base.multiply(BigDecimal("2.5"))    // max = 2.5× optimistic
        } else {
            // Fall back to TVL bucket
            val tvl = proto.tvl ?: 0.0
            when {
                tvl > 1_000_000_000 -> BigDecimal("2000")  to BigDecimal("8000")
                tvl > 100_000_000   -> BigDecimal("500")   to BigDecimal("3000")
                tvl > 10_000_000    -> BigDecimal("100")   to BigDecimal("1000")
                else                -> BigDecimal("50")    to BigDecimal("500")
            }
        }
    }

    private fun resolveStatus(proto: LlamaProtocol): AirdropStatus {
        val listedAt = proto.listedAt
        return when {
            listedAt == null -> AirdropStatus.LIVE
            listedAt * 1000 > System.currentTimeMillis() -> AirdropStatus.SOON
            else -> AirdropStatus.LIVE
        }
    }

    private fun resolveDeadline(proto: LlamaProtocol): OffsetDateTime? {
        // Rough heuristic: 90 days from listing, or 60 days from now if no listing date
        val listedAt = proto.listedAt
        return if (listedAt != null) {
            OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(listedAt),
                ZoneOffset.UTC
            ).plusDays(90)
        } else {
            OffsetDateTime.now(ZoneOffset.UTC).plusDays(60)
        }
    }

    private fun buildEntity(proto: LlamaProtocol, priceMap: Map<String, Double>): AirdropEntity {
        val category = resolveCategory(proto)
        val (minVal, maxVal) = estimateValueRange(proto, priceMap)
        return AirdropEntity(
            name = proto.name,
            symbol = proto.symbol ?: "?",
            logoUrl = proto.logo,
            estimatedValueMin = minVal,
            estimatedValueMax = maxVal,
            status = resolveStatus(proto),
            category = category,
            deadline = resolveDeadline(proto),
            steps = resolveSteps(category),
            isHot = maxVal >= BigDecimal("2000"),
            isPro = false,
            llamaSlug = proto.slug ?: proto.id,
            lastRefreshedAt = OffsetDateTime.now(ZoneOffset.UTC)
        )
    }

    // ── Persistence (upsert) ──────────────────────────────────────────────────

    @Transactional
    fun upsertAll(entities: List<AirdropEntity>) {
        val newAlerts = mutableListOf<AirdropEntity>()
        val hotAlerts = mutableListOf<AirdropEntity>()
        val now = OffsetDateTime.now(ZoneOffset.UTC)

        // Don't blast alerts on the very first sync to avoid spamming 50+ messages
        val isFirstSync = repo.countLive() == 0L

        entities.forEach { incoming ->
            val slug = incoming.llamaSlug
            if (slug != null) {
                val existingOpt = repo.findByLlamaSlug(slug)
                if (existingOpt.isPresent) {
                    val existing = existingOpt.get()
                    val becameHot = incoming.isHot && !existing.isHot && existing.notifiedHotAt == null
                    
                    val updated = incoming.copy(
                        id = existing.id,
                        createdAt = existing.createdAt,
                        notifiedNewAt = existing.notifiedNewAt,
                        notifiedHotAt = if (becameHot) now else existing.notifiedHotAt,
                        notifiedDeadlineAt = existing.notifiedDeadlineAt
                    )
                    val saved = repo.save(updated)
                    if (becameHot && !isFirstSync) hotAlerts.add(saved)
                } else {
                    val toSave = incoming.copy(
                        notifiedNewAt = now,
                        notifiedHotAt = if (incoming.isHot) now else null
                    )
                    val saved = repo.save(toSave)
                    if (!isFirstSync) {
                        newAlerts.add(saved)
                        if (incoming.isHot) hotAlerts.add(saved)
                    }
                }
            } else {
                repo.save(incoming)
            }
        }

        if (newAlerts.isNotEmpty()) telegramAlertService.notifyNewAirdrops(newAlerts)
        if (hotAlerts.isNotEmpty()) telegramAlertService.notifyHotAirdrops(hotAlerts)
    }

    // ── Query helpers (used by controllers) ───────────────────────────────────

    fun getLiveAirdrops(): List<AirdropEntity> =
        repo.findAllByStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE)

    fun getHotAirdrops(): List<AirdropEntity> =
        repo.findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE)

    fun getByCategory(category: AirdropCategory): List<AirdropEntity> =
        repo.findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc(AirdropStatus.LIVE, category)

    fun getEndingSoon(withinHours: Long = 72): List<AirdropEntity> {
        val threshold = OffsetDateTime.now(ZoneOffset.UTC).plusHours(withinHours)
        return repo.findEndingSoon(AirdropStatus.LIVE, threshold)
    }

    // ── Fallback mock data ────────────────────────────────────────────────────

    @Transactional
    fun applyFallbackData() {
        if (repo.countLive() > 0) {
            log.info("Fallback skipped — existing live data is present")
            return
        }
        log.warn("Writing fallback mock data to database (API unavailable)")
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val mocks = listOf(
            AirdropEntity(
                name = "LayerZero", symbol = "ZRO", logoUrl = null,
                estimatedValueMin  = BigDecimal("1000"), estimatedValueMax = BigDecimal("4000"),
                status = AirdropStatus.LIVE, category = AirdropCategory.BRIDGE,
                deadline = now.plusDays(12), steps = BRIDGE_STEPS,
                isHot = true, isPro = false, llamaSlug = "layerzero-fallback"
            ),
            AirdropEntity(
                name = "zkSync Era", symbol = "ZK", logoUrl = null,
                estimatedValueMin  = BigDecimal("800"), estimatedValueMax = BigDecimal("3000"),
                status = AirdropStatus.LIVE, category = AirdropCategory.L2,
                deadline = now.plusDays(20), steps = L2_STEPS,
                isHot = true, isPro = false, llamaSlug = "zksync-era-fallback"
            ),
            AirdropEntity(
                name = "EigenLayer", symbol = "EIGEN", logoUrl = null,
                estimatedValueMin  = BigDecimal("1500"), estimatedValueMax = BigDecimal("5000"),
                status = AirdropStatus.LIVE, category = AirdropCategory.L2,
                deadline = now.plusDays(15), steps = L2_STEPS,
                isHot = true, isPro = false, llamaSlug = "eigenlayer-fallback"
            ),
            AirdropEntity(
                name = "Scroll", symbol = "SCR", logoUrl = null,
                estimatedValueMin  = BigDecimal("400"), estimatedValueMax = BigDecimal("1500"),
                status = AirdropStatus.LIVE, category = AirdropCategory.L2,
                deadline = now.plusDays(25), steps = L2_STEPS,
                isHot = false, isPro = false, llamaSlug = "scroll-fallback"
            ),
            AirdropEntity(
                name = "Blast", symbol = "BLAST", logoUrl = null,
                estimatedValueMin  = BigDecimal("500"), estimatedValueMax = BigDecimal("2000"),
                status = AirdropStatus.LIVE, category = AirdropCategory.L2,
                deadline = now.plusDays(10), steps = L2_STEPS,
                isHot = true, isPro = false, llamaSlug = "blast-fallback"
            ),
            AirdropEntity(
                name = "Hyperliquid", symbol = "HYPE", logoUrl = null,
                estimatedValueMin  = BigDecimal("2000"), estimatedValueMax = BigDecimal("6000"),
                status = AirdropStatus.LIVE, category = AirdropCategory.DEFI,
                deadline = now.plusDays(2), steps = DEFI_STEPS,
                isHot = true, isPro = false, llamaSlug = "hyperliquid-fallback"
            ),
        )
        mocks.forEach { mock ->
            if (repo.findByLlamaSlug(mock.llamaSlug!!).isEmpty) {
                repo.save(mock)
            }
        }
        log.info("Fallback: wrote ${mocks.size} mock AirdropEntity records")
    }
}
