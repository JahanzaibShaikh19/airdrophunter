package com.airdrophunter.dto

import com.airdrophunter.domain.AirdropCategory
import com.airdrophunter.domain.AirdropEntity
import com.airdrophunter.domain.AirdropStatus
import java.math.BigDecimal
import java.time.OffsetDateTime

data class AirdropEntityDto(
    val id: Long,
    val name: String,
    val symbol: String,
    val logoUrl: String?,
    val estimatedValueMin: BigDecimal,
    val estimatedValueMax: BigDecimal,
    val status: AirdropStatus,
    val category: AirdropCategory,
    val deadline: OffsetDateTime?,
    val steps: List<String>,
    val isHot: Boolean,
    val isPro: Boolean,
    val lastRefreshedAt: OffsetDateTime
) {
    companion object {
        fun from(entity: AirdropEntity) = AirdropEntityDto(
            id = entity.id,
            name = entity.name,
            symbol = entity.symbol,
            logoUrl = entity.logoUrl,
            estimatedValueMin = entity.estimatedValueMin,
            estimatedValueMax = entity.estimatedValueMax,
            status = entity.status,
            category = entity.category,
            deadline = entity.deadline,
            steps = entity.steps,
            isHot = entity.isHot,
            isPro = entity.isPro,
            lastRefreshedAt = entity.lastRefreshedAt
        )
    }
}
