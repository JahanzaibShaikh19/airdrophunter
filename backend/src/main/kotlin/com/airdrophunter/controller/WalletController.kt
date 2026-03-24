package com.airdrophunter.controller

import com.airdrophunter.dto.WalletCheckRequest
import com.airdrophunter.dto.WalletCheckResponse
import com.airdrophunter.service.AirdropService
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = ["*"])
class WalletController(private val service: AirdropService) {

    @PostMapping("/check")
    fun checkWallet(
        @Valid @RequestBody request: WalletCheckRequest
    ): ResponseEntity<WalletCheckResponse> = runBlocking {
        ResponseEntity.ok(service.checkWallet(request))
    }
}
