package com.airdrophunter.dto

import java.math.BigDecimal
import java.time.OffsetDateTime

data class AirdropDto(
    val id: Long,
    val name: String,
    val description: String,
    val token: String,
    val protocol: String,
    val chain: String,
    val estimatedValue: BigDecimal,
    val endsAt: OffsetDateTime,
    val isActive: Boolean,
    val websiteUrl: String?,
    val createdAt: OffsetDateTime
)

data class WalletCheckRequest(
    val address: String
)

data class WalletCheckResponse(
    val address: String,
    val eligible: Boolean,
    val reason: String,
    val estimatedReward: BigDecimal,
    val eligibleAirdrops: List<String>
)

data class StatsDto(
    val totalActive: Long,
    val totalEstimatedValue: BigDecimal,
    val endingToday: Long
)
