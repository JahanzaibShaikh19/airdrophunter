package com.airdrophunter.controller

import com.airdrophunter.dto.AirdropDto
import com.airdrophunter.dto.StatsDto
import com.airdrophunter.dto.AirdropEligibility
import com.airdrophunter.dto.WalletResult
import com.airdrophunter.service.AirdropService
import com.airdrophunter.service.WalletService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

// ── Shared mock service bean ─────────────────────────────────────────────────

@TestConfiguration
class MockServiceConfig {
    @Bean
    fun airdropService(): AirdropService = mockk()

    @Bean
    fun walletService(): WalletService = mockk()
}

// ── Fixture ──────────────────────────────────────────────────────────────────

private fun sampleDto(id: Long = 1L, name: String = "LayerZero") = AirdropDto(
    id = id,
    name = name,
    description = "Test description",
    token = "ZRO",
    protocol = "LayerZero",
    chain = "Multi-chain",
    estimatedValue = BigDecimal("2500.00"),
    endsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10),
    isActive = true,
    websiteUrl = "https://layerzero.network",
    createdAt = OffsetDateTime.now(ZoneOffset.UTC)
)

// ── AirdropController ────────────────────────────────────────────────────────

@WebMvcTest(AirdropController::class)
@Import(MockServiceConfig::class)
@DisplayName("AirdropController")
class AirdropControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var service: AirdropService

    @Nested
    @DisplayName("GET /api/airdrops")
    inner class GetAll {

        @Test
        fun `returns 200 and JSON array`() {
            coEvery { service.getAllActive() } returns listOf(sampleDto(), sampleDto(2L, "zkSync"))

            mvc.get("/api/airdrops")
                .andExpect {
                    status { isOk() }
                    content { contentType(MediaType.APPLICATION_JSON) }
                    jsonPath("$.length()") { value(2) }
                    jsonPath("$[0].name") { value("LayerZero") }
                    jsonPath("$[1].name") { value("zkSync") }
                }
        }

        @Test
        fun `returns empty array when no airdrops`() {
            coEvery { service.getAllActive() } returns emptyList()

            mvc.get("/api/airdrops")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(0) }
                }
        }

        @Test
        fun `response includes all DTO fields`() {
            coEvery { service.getAllActive() } returns listOf(sampleDto())

            mvc.get("/api/airdrops")
                .andExpect {
                    status { isOk() }
                    jsonPath("$[0].id") { value(1) }
                    jsonPath("$[0].token") { value("ZRO") }
                    jsonPath("$[0].chain") { value("Multi-chain") }
                    jsonPath("$[0].estimatedValue") { value(2500.0) }
                    jsonPath("$[0].isActive") { value(true) }
                    jsonPath("$[0].websiteUrl") { value("https://layerzero.network") }
                }
        }
    }

    @Nested
    @DisplayName("GET /api/airdrops/hot")
    inner class GetHot {

        @Test
        fun `returns 200 and sorted JSON array`() {
            val drops = listOf(
                sampleDto(1L, "Hyperliquid").copy(estimatedValue = BigDecimal("4200.00")),
                sampleDto(2L, "EigenLayer").copy(estimatedValue = BigDecimal("3100.00")),
                sampleDto(3L, "LayerZero").copy(estimatedValue = BigDecimal("2500.00"))
            )
            coEvery { service.getHot() } returns drops

            mvc.get("/api/airdrops/hot")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.length()") { value(3) }
                    jsonPath("$[0].name") { value("Hyperliquid") }
                    jsonPath("$[0].estimatedValue") { value(4200.0) }
                    jsonPath("$[1].name") { value("EigenLayer") }
                }
        }
    }
}

// ── WalletController ─────────────────────────────────────────────────────────

@WebMvcTest(WalletController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MockServiceConfig::class)
@DisplayName("WalletController")
class WalletControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var service: WalletService
    @Autowired lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST check returns 200 with eligible response`() {
        val response = WalletResult(
            address = "0xd8da6bf26964af9d7eed9e03e53415d37aa96045",
            eligibleAirdrops = listOf(
                AirdropEligibility(
                    airdropName = "LayerZero Airdrop",
                    protocol = "LayerZero",
                    estimatedValue = "$2500.00",
                    reason = "Strong cross-chain profile"
                )
            ),
            totalEstimatedValue = "$2500.00",
            recommendations = listOf("Use zkSync Era more frequently")
        )
        every { service.checkWallet(any()) } returns response

        mvc.post("/api/wallet/check") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"address":"0xd8da6bf26964af9d7eed9e03e53415d37aa96045"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.address") { value("0xd8da6bf26964af9d7eed9e03e53415d37aa96045") }
            jsonPath("$.totalEstimatedValue") { value("$2500.00") }
            jsonPath("$.eligibleAirdrops.length()") { value(1) }
            jsonPath("$.eligibleAirdrops[0].airdropName") { value("LayerZero Airdrop") }
        }
    }

    @Test
    fun `POST check returns 200 with ineligible response`() {
        val response = WalletResult(
            address = "0x0000000000000000000000000000000000000001",
            eligibleAirdrops = emptyList(),
            totalEstimatedValue = "$0.00",
            recommendations = listOf("Increase Ethereum mainnet activity")
        )
        every { service.checkWallet(any()) } returns response

        mvc.post("/api/wallet/check") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"address":"0x0000000000000000000000000000000000000001"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.totalEstimatedValue") { value("$0.00") }
            jsonPath("$.eligibleAirdrops.length()") { value(0) }
        }
    }

    @Test
    fun `POST check returns 400 for bad wallet input`() {
        every { service.checkWallet(any()) } throws org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.BAD_REQUEST,
            "Wallet address must be a valid Ethereum address"
        )

        mvc.post("/api/wallet/check") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"address":"0xtest"}"""
        }.andExpect { status { isBadRequest() } }
    }
}

// ── StatsController ───────────────────────────────────────────────────────────

@WebMvcTest(StatsController::class)
@Import(MockServiceConfig::class)
@DisplayName("StatsController")
class StatsControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var service: AirdropService

    @Test
    fun `GET stats returns 200 with correct fields`() {
        coEvery { service.getStats() } returns StatsDto(
            totalActive = 10L,
            totalEstimatedValue = BigDecimal("16340.00"),
            endingToday = 2L
        )

        mvc.get("/api/stats")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.totalActive") { value(10) }
                jsonPath("$.totalEstimatedValue") { value(16340.0) }
                jsonPath("$.endingToday") { value(2) }
            }
    }

    @Test
    fun `GET stats returns zeros when no data`() {
        coEvery { service.getStats() } returns StatsDto(
            totalActive = 0L,
            totalEstimatedValue = BigDecimal.ZERO,
            endingToday = 0L
        )

        mvc.get("/api/stats")
            .andExpect {
                status { isOk() }
                jsonPath("$.totalActive") { value(0) }
                jsonPath("$.endingToday") { value(0) }
            }
    }
}
