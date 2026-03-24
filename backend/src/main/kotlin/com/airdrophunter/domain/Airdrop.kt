package com.airdrophunter.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "airdrops")
data class Airdrop(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 120)
    val name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, length = 20)
    val token: String,

    @Column(nullable = false, length = 80)
    val protocol: String,

    @Column(nullable = false, length = 40)
    val chain: String = "Ethereum",

    @Column(name = "estimated_value", nullable = false, precision = 18, scale = 2)
    val estimatedValue: BigDecimal = BigDecimal.ZERO,

    @Column(name = "ends_at", nullable = false)
    val endsAt: OffsetDateTime,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "website_url", length = 255)
    val websiteUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
