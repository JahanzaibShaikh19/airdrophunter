package com.airdrophunter.controller

import com.airdrophunter.service.ProUserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

data class ActivateRequest(
    @field:Email(message = "Must be a valid email")
    @field:NotBlank
    val email: String,

    @field:NotBlank(message = "License key must not be blank")
    val licenseKey: String
)

data class ActivateResponse(
    val token: String,
    val email: String,
    val expiresAt: Long   // Unix epoch seconds
)

/**
 * Handles PRO license activation and JWT issuance.
 *
 * POST /api/auth/activate  { email, licenseKey } → { token, email, expiresAt }
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
class AuthController(private val proUserService: ProUserService) {

    @PostMapping("/activate")
    fun activate(@Valid @RequestBody req: ActivateRequest): ResponseEntity<*> {
        return try {
            val token = proUserService.activateAndIssueToken(req.email, req.licenseKey)
            // 30-day expiry (matches JwtService default)
            val expiresAt = Instant.now().epochSecond + (30L * 24 * 60 * 60)
            ResponseEntity.ok(ActivateResponse(token = token, email = req.email.lowercase().trim(), expiresAt = expiresAt))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(401).body(mapOf("error" to (ex.message ?: "Invalid credentials")))
        }
    }
}
