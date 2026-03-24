package com.airdrophunter.repository

import com.airdrophunter.domain.AirdropCategory
import com.airdrophunter.domain.AirdropEntity
import com.airdrophunter.domain.AirdropStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.Optional

@Repository
interface AirdropEntityRepository : JpaRepository<AirdropEntity, Long> {

    fun findAllByStatusOrderByEstimatedValueMaxDesc(status: AirdropStatus): List<AirdropEntity>

    fun findAllByStatusAndCategoryOrderByEstimatedValueMaxDesc(
        status: AirdropStatus,
        category: AirdropCategory
    ): List<AirdropEntity>

    fun findAllByIsHotTrueAndStatusOrderByEstimatedValueMaxDesc(status: AirdropStatus): List<AirdropEntity>

    fun findByLlamaSlug(slug: String): Optional<AirdropEntity>

    @Query("SELECT a FROM AirdropEntity a WHERE a.status = :status AND a.deadline <= :threshold ORDER BY a.deadline ASC")
    fun findEndingSoon(status: AirdropStatus, threshold: OffsetDateTime): List<AirdropEntity>

    @Query("SELECT COUNT(a) FROM AirdropEntity a WHERE a.status = 'LIVE'")
    fun countLive(): Long

    @Query("SELECT COALESCE(SUM(a.estimatedValueMax), 0) FROM AirdropEntity a WHERE a.status = 'LIVE'")
    fun sumMaxValueLive(): java.math.BigDecimal

    @Modifying
    @Transactional
    @Query("UPDATE AirdropEntity a SET a.status = 'ENDED' WHERE a.deadline < :now AND a.status <> 'ENDED'")
    fun markExpiredAsEnded(now: OffsetDateTime): Int
}
