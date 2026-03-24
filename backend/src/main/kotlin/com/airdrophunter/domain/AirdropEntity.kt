package com.airdrophunter.domain

import com.airdrophunter.domain.converter.StringListConverter
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Persisted representation of a DeFiLlama-sourced airdrop opportunity.
 * Stored in the [defi_airdrops] table, refreshed every 15 minutes by
 * [com.airdrophunter.service.DefiLlamaService].
 */
@Entity
@Table(name = "defi_airdrops")
data class AirdropEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Human-readable protocol name, e.g. "LayerZero" */
    @Column(nullable = false, length = 160)
    val name: String,

    /** Token ticker symbol, e.g. "ZRO" */
    @Column(nullable = false, length = 20)
    val symbol: String,

    /** CDN URL of the protocol logo from DeFiLlama */
    @Column(name = "logo_url", length = 512)
    val logoUrl: String? = null,

    /** Lower bound of estimated USD airdrop value per wallet */
    @Column(name = "estimated_value_min", nullable = false, precision = 20, scale = 2)
    val estimatedValueMin: BigDecimal = BigDecimal.ZERO,

    /** Upper bound of estimated USD airdrop value per wallet */
    @Column(name = "estimated_value_max", nullable = false, precision = 20, scale = 2)
    val estimatedValueMax: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val status: AirdropStatus = AirdropStatus.LIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val category: AirdropCategory = AirdropCategory.OTHER,

    /** Unix-style deadline; null = no announced deadline */
    @Column(name = "deadline")
    val deadline: OffsetDateTime? = null,

    /**
     * Ordered list of on-chain steps a user must perform.
     * Persisted as pipe-delimited TEXT via [StringListConverter].
     */
    @Convert(converter = StringListConverter::class)
    @Column(nullable = false, columnDefinition = "TEXT")
    val steps: List<String> = emptyList(),

    /** True if estimated value is in the top-10% of active airdrops */
    @Column(name = "is_hot", nullable = false)
    val isHot: Boolean = false,

    /** True if airdrop requires paid tooling / premium access */
    @Column(name = "is_pro", nullable = false)
    val isPro: Boolean = false,

    /**
     * Unique slug from DeFiLlama, used as the upsert key to avoid duplicates
     * on every 15-minute refresh cycle.
     */
    @Column(name = "llama_slug", length = 200, unique = true)
    val llamaSlug: String? = null,

    @Column(name = "last_refreshed_at", nullable = false)
    val lastRefreshedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
