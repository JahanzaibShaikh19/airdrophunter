package com.airdrophunter.controller

import com.airdrophunter.security.JwtAuthFilter
import com.airdrophunter.service.ProUserService
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.post
import com.airdrophunter.config.SecurityConfig

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class, JwtAuthFilter::class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var mapper: ObjectMapper
    @MockkBean lateinit var proUserService: ProUserService
    @MockkBean lateinit var jwtService: com.airdrophunter.security.JwtService

    private fun body(email: String, key: String) =
        mapper.writeValueAsString(mapOf("email" to email, "licenseKey" to key))

    @Test
    fun `POST activate returns 200 and token on valid credentials`() {
        every { proUserService.activateAndIssueToken("pro@example.com", "VALID-KEY") } returns "jwt.pro.token"

        mvc.post("/api/auth/activate") {
            contentType = MediaType.APPLICATION_JSON
            content = body("pro@example.com", "VALID-KEY")
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { value("jwt.pro.token") }
            jsonPath("$.email") { value("pro@example.com") }
            jsonPath("$.expiresAt") { isNumber() }
        }
    }

    @Test
    fun `POST activate returns 401 on invalid credentials`() {
        every { proUserService.activateAndIssueToken(any(), any()) } throws
                IllegalArgumentException("Invalid license key")

        mvc.post("/api/auth/activate") {
            contentType = MediaType.APPLICATION_JSON
            content = body("user@x.com", "BAD-KEY")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { value("Invalid license key") }
        }
    }

    @Test
    fun `POST activate returns 400 on blank email`() {
        mvc.post("/api/auth/activate") {
            contentType = MediaType.APPLICATION_JSON
            content = body("", "VALID-KEY")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `POST activate returns 400 on invalid email format`() {
        mvc.post("/api/auth/activate") {
            contentType = MediaType.APPLICATION_JSON
            content = body("not-an-email", "VALID-KEY")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `POST activate returns 400 on blank license key`() {
        mvc.post("/api/auth/activate") {
            contentType = MediaType.APPLICATION_JSON
            content = body("user@x.com", "")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `POST activate normalises email to lowercase in token response`() {
        every { proUserService.activateAndIssueToken("mixed@case.com", "KEY") } returns "tok"

        mvc.post("/api/auth/activate") {
            contentType = MediaType.APPLICATION_JSON
            content = body("MIXED@CASE.COM", "KEY")
        }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("mixed@case.com") }
        }
    }
}
