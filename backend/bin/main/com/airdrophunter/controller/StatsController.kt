package com.airdrophunter.controller

import com.airdrophunter.dto.StatsDto
import com.airdrophunter.service.AirdropService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = ["*"])
class StatsController(private val service: AirdropService) {

    @GetMapping
    fun getStats(): ResponseEntity<StatsDto> = runBlocking {
        ResponseEntity.ok(service.getStats())
    }
}
