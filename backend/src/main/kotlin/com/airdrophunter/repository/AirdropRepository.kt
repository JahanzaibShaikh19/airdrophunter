package com.airdrophunter.repository

import com.airdrophunter.domain.Airdrop
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
interface AirdropRepository : JpaRepository<Airdrop, Long> {

    fun findAllByIsActiveTrueOrderByCreatedAtDesc(): List<Airdrop>

    fun findAllByIsActiveTrueOrderByEstimatedValueDesc(): List<Airdrop>

    @Query("""
        SELECT a FROM Airdrop a
        WHERE a.isActive = true
          AND a.endsAt >= :startOfDay
          AND a.endsAt < :endOfDay
        ORDER BY a.estimatedValue DESC
    """)
    fun findEndingToday(startOfDay: OffsetDateTime, endOfDay: OffsetDateTime): List<Airdrop>

    fun countByIsActiveTrue(): Long

    @Query("SELECT COALESCE(SUM(a.estimatedValue), 0) FROM Airdrop a WHERE a.isActive = true")
    fun sumEstimatedValueActive(): java.math.BigDecimal
}
