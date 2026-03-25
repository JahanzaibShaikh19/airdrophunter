package com.airdrophunter.controller

import com.airdrophunter.security.JwtAuthFilter
import com.airdrophunter.service.GumroadWebhookService
import com.airdrophunter.service.WebhookResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import com.airdrophunter.config.SecurityConfig

@WebMvcTest(GumroadWebhookController::class)
@Import(SecurityConfig::class, JwtAuthFilter::class)
@DisplayName("GumroadWebhookController")
class GumroadWebhookControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @MockkBean lateinit var webhookService: GumroadWebhookService
    @MockkBean lateinit var jwtService: com.airdrophunter.security.JwtService

    private val validBody = "email=buyer%40example.com&license_key=LK-001&sale_id=S1"

    @Test
    fun `GET gumroad ping returns 200 with ok status`() {
        mvc.get("/api/webhook/gumroad/ping")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("ok") }
                jsonPath("$.service") { value("AirdropHunter Webhook") }
            }
    }

    @Test
    fun `POST gumroad returns 401 when signature header is missing`() {
        mvc.post("/api/webhook/gumroad") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            content = validBody
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `POST gumroad returns 401 when signature is invalid`() {
        every { webhookService.verifySignature(any(), "bad-sig") } returns false

        mvc.post("/api/webhook/gumroad") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            content = validBody
            header("X-Gumroad-Signature", "bad-sig")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { value("Invalid signature") }
        }
    }

    @Test
    fun `POST gumroad returns 200 on valid signature and successful event`() {
        every { webhookService.verifySignature(any(), "valid-sig") } returns true
        every { webhookService.processSaleEvent(any()) } returns
                WebhookResult(success = true, message = "PRO activated for buyer@example.com")

        mvc.post("/api/webhook/gumroad") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            content = validBody
            header("X-Gumroad-Signature", "valid-sig")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ok") }
        }
    }

    @Test
    fun `POST gumroad returns 200 with ignored status on bad payload`() {
        every { webhookService.verifySignature(any(), "valid-sig") } returns true
        every { webhookService.processSaleEvent(any()) } returns
                WebhookResult(success = false, message = "Missing email in payload")

        mvc.post("/api/webhook/gumroad") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            content = "no_email=here"
            header("X-Gumroad-Signature", "valid-sig")
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("ignored") }
        }
    }

    @Test
    fun `POST gumroad invokes processSaleEvent with flattened params`() {
        every { webhookService.verifySignature(any(), any()) } returns true
        every { webhookService.processSaleEvent(any()) } returns WebhookResult(true, "ok")

        mvc.post("/api/webhook/gumroad") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            content = validBody
            header("X-Gumroad-Signature", "sig")
        }

        verify { webhookService.processSaleEvent(match { it["sale_id"] == "S1" }) }
    }
}
