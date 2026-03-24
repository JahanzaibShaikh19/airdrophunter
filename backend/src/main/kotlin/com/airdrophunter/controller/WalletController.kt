package com.airdrophunter.controller

import com.airdrophunter.dto.WalletCheckRequest
import com.airdrophunter.dto.WalletResult
import com.airdrophunter.service.WalletService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = ["*"])
class WalletController(private val service: WalletService) {

    @PostMapping("/check")
    fun checkWallet(
        @Valid @RequestBody request: WalletCheckRequest
    ): ResponseEntity<WalletResult> {
        ResponseEntity.ok(service.checkWallet(request))
    }
}
