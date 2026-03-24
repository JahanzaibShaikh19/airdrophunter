package com.airdrophunter.controller

import com.airdrophunter.dto.AirdropDto
import com.airdrophunter.service.AirdropService
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/airdrops")
@CrossOrigin(origins = ["*"])
class AirdropController(private val service: AirdropService) {

    @GetMapping
    fun getAllActive(): ResponseEntity<List<AirdropDto>> = runBlocking {
        ResponseEntity.ok(service.getAllActive())
    }

    @GetMapping("/hot")
    fun getHot(): ResponseEntity<List<AirdropDto>> = runBlocking {
        ResponseEntity.ok(service.getHot())
    }
}
