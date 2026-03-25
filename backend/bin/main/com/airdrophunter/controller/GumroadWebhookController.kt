package com.airdrophunter.controller

import com.airdrophunter.service.GumroadWebhookService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.*

/**
 * Receives Gumroad sale/refund webhook pings.
 *
 * Gumroad sends a POST with:
 *  Content-Type: application/x-www-form-urlencoded
 *  X-Gumroad-Signature: <hmac-sha256-hex>
 *
 * Returns 200 on success (Gumroad expects a 2xx to mark the ping as delivered).
 * Returns 401 on signature failure so Gumroad retries later.
 */
@RestController
@RequestMapping("/api/webhook")
class GumroadWebhookController(private val webhookService: GumroadWebhookService) {

    private val log = LoggerFactory.getLogger(GumroadWebhookController::class.java)

    @PostMapping("/gumroad", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun handleGumroadPing(
        @RequestParam params: MultiValueMap<String, String>,
        @RequestHeader(value = "X-Gumroad-Signature", required = false) signature: String?,
        request: HttpServletRequest
    ): ResponseEntity<Map<String, String>> {

        // Read the raw body for HMAC verification
        val rawBody = request.inputStream.readBytes()

        if (signature == null) {
            log.warn("Gumroad webhook received without X-Gumroad-Signature header")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Missing signature header"))
        }

        if (!webhookService.verifySignature(rawBody, signature)) {
            log.warn("Gumroad webhook signature mismatch — possible spoofed request")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid signature"))
        }

        // Flatten MultiValueMap → Map<String, String> (single value per key)
        val flatParams = params.toSingleValueMap()

        log.info("Processing Gumroad webhook: sale_id=${flatParams["sale_id"]}, email=${flatParams["email"]}")
        val result = webhookService.processSaleEvent(flatParams)

        return if (result.success) {
            ResponseEntity.ok(mapOf("status" to "ok", "message" to result.message))
        } else {
            // Return 200 anyway so Gumroad doesn't keep retrying a fundamentally bad payload
            ResponseEntity.ok(mapOf("status" to "ignored", "message" to result.message))
        }
    }

    /** Test connectivity endpoint — Gumroad can ping this to verify the URL */
    @GetMapping("/gumroad/ping")
    fun ping(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(mapOf("status" to "ok", "service" to "AirdropHunter Webhook"))
}
