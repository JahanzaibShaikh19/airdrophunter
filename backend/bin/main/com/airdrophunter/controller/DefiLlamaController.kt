package com.airdrophunter.controller

import com.airdrophunter.domain.AirdropCategory
import com.airdrophunter.dto.AirdropEntityDto
import com.airdrophunter.service.DefiLlamaService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Exposes live DeFiLlama-sourced airdrop data.
 *
 * Endpoints:
 *  GET /api/defi/airdrops           — all LIVE airdrops (value desc)
 *  GET /api/defi/airdrops/hot       — only isHot=true airdrops
 *  GET /api/defi/airdrops/soon      — ending within 72 h
 *  GET /api/defi/airdrops/category/{cat} — filtered by AirdropCategory
 *  POST /api/defi/airdrops/refresh  — manual trigger (admin use)
 */
@RestController
@RequestMapping("/api/defi")
@CrossOrigin(origins = ["*"])
class DefiLlamaController(private val service: DefiLlamaService) {

    private val log = LoggerFactory.getLogger(DefiLlamaController::class.java)

    @GetMapping("/airdrops")
    fun getLive(): ResponseEntity<List<AirdropEntityDto>> {
        val result = service.getLiveAirdrops().map(AirdropEntityDto::from)
        log.debug("GET /api/defi/airdrops → ${result.size} records")
        return ResponseEntity.ok(result)
    }

    @GetMapping("/airdrops/hot")
    fun getHot(): ResponseEntity<List<AirdropEntityDto>> {
        val result = service.getHotAirdrops().map(AirdropEntityDto::from)
        log.debug("GET /api/defi/airdrops/hot → ${result.size} records")
        return ResponseEntity.ok(result)
    }

    @GetMapping("/airdrops/soon")
    fun getEndingSoon(
        @RequestParam(defaultValue = "72") withinHours: Long
    ): ResponseEntity<List<AirdropEntityDto>> {
        val result = service.getEndingSoon(withinHours).map(AirdropEntityDto::from)
        log.debug("GET /api/defi/airdrops/soon (within ${withinHours}h) → ${result.size} records")
        return ResponseEntity.ok(result)
    }

    @GetMapping("/airdrops/category/{category}")
    fun getByCategory(
        @PathVariable category: String
    ): ResponseEntity<List<AirdropEntityDto>> {
        val cat = try {
            AirdropCategory.valueOf(category.uppercase())
        } catch (e: IllegalArgumentException) {
            log.warn("Unknown category requested: $category")
            return ResponseEntity.badRequest().build()
        }
        val result = service.getByCategory(cat).map(AirdropEntityDto::from)
        log.debug("GET /api/defi/airdrops/category/$category → ${result.size} records")
        return ResponseEntity.ok(result)
    }

    /** Manual refresh — useful during development / admin ops */
    @PostMapping("/airdrops/refresh")
    fun triggerRefresh(): ResponseEntity<Map<String, String>> {
        log.info("Manual DeFiLlama refresh triggered via API")
        kotlinx.coroutines.runBlocking {
            service.refreshAirdrops()
        }
        return ResponseEntity.ok(mapOf("status" to "refresh triggered"))
    }
}
