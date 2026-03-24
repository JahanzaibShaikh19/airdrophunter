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

data class AirdropEligibility(
    val airdropName: String,
    val protocol: String,
    val estimatedValue: String,
    val reason: String
)

data class WalletResult(
    val address: String,
    val eligibleAirdrops: List<AirdropEligibility>,
    val totalEstimatedValue: String,
    val recommendations: List<String>
)

data class StatsDto(
    val totalActive: Long,
    val totalEstimatedValue: BigDecimal,
    val endingToday: Long
)
