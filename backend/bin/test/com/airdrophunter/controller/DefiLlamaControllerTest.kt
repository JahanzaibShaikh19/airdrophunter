package com.airdrophunter.controller

import com.airdrophunter.domain.AirdropCategory
import com.airdrophunter.domain.AirdropEntity
import com.airdrophunter.domain.AirdropStatus
import com.airdrophunter.service.DefiLlamaService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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

@TestConfiguration
class DefiMockConfig {
    @Bean
    fun defiLlamaService(): DefiLlamaService = mockk(relaxed = true)
}

private fun sampleEntity(
    id: Long = 1L,
    name: String = "LayerZero",
    isHot: Boolean = true,
    category: AirdropCategory = AirdropCategory.BRIDGE
) = AirdropEntity(
    id = id, name = name, symbol = "ZRO", logoUrl = null,
    estimatedValueMin = BigDecimal("1000"), estimatedValueMax = BigDecimal("4000"),
    status = AirdropStatus.LIVE, category = category,
    deadline = OffsetDateTime.now(ZoneOffset.UTC).plusDays(12),
    steps = listOf("Bridge ETH", "Swap on DEX", "Check eligibility"),
    isHot = isHot, isPro = false, llamaSlug = "layerzero",
    lastRefreshedAt = OffsetDateTime.now(ZoneOffset.UTC)
)

@WebMvcTest(DefiLlamaController::class)
@Import(DefiMockConfig::class)
@DisplayName("DefiLlamaController")
class DefiLlamaControllerTest {

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var service: DefiLlamaService

    @Test
    fun `GET airdrops returns 200 and list`() {
        every { service.getLiveAirdrops() } returns listOf(sampleEntity(), sampleEntity(2L, "zkSync"))

        mvc.get("/api/defi/airdrops")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.length()") { value(2) }
                jsonPath("$[0].name") { value("LayerZero") }
                jsonPath("$[0].symbol") { value("ZRO") }
                jsonPath("$[0].isHot") { value(true) }
                jsonPath("$[0].steps.length()") { value(3) }
            }
    }

    @Test
    fun `GET airdrops returns all DTO fields`() {
        every { service.getLiveAirdrops() } returns listOf(sampleEntity())

        mvc.get("/api/defi/airdrops")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].id") { value(1) }
                jsonPath("$[0].estimatedValueMin") { value(1000.0) }
                jsonPath("$[0].estimatedValueMax") { value(4000.0) }
                jsonPath("$[0].status") { value("LIVE") }
                jsonPath("$[0].category") { value("BRIDGE") }
                jsonPath("$[0].isPro") { value(false) }
            }
    }

    @Test
    fun `GET airdrops returns empty list when no live airdrops`() {
        every { service.getLiveAirdrops() } returns emptyList()

        mvc.get("/api/defi/airdrops")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(0) }
            }
    }

    @Test
    fun `GET airdrops hot returns only hot entities`() {
        every { service.getHotAirdrops() } returns listOf(sampleEntity(isHot = true))

        mvc.get("/api/defi/airdrops/hot")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
                jsonPath("$[0].isHot") { value(true) }
            }
    }

    @Test
    fun `GET airdrops soon returns ending-soon list`() {
        every { service.getEndingSoon(72) } returns listOf(sampleEntity())

        mvc.get("/api/defi/airdrops/soon?withinHours=72")
            .andExpect {
                status { isOk() }
                jsonPath("$.length()") { value(1) }
            }
    }

    @Test
    fun `GET airdrops category L2 returns filtered list`() {
        every { service.getByCategory(AirdropCategory.L2) } returns
                listOf(sampleEntity(category = AirdropCategory.L2))

        mvc.get("/api/defi/airdrops/category/L2")
            .andExpect {
                status { isOk() }
                jsonPath("$[0].category") { value("L2") }
            }
    }

    @Test
    fun `GET airdrops category with unknown value returns 400`() {
        mvc.get("/api/defi/airdrops/category/UNKNOWN_CATEGORY")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `POST refresh returns 200 with status message`() {
        every { service.getLiveAirdrops() } returns emptyList()

        mvc.post("/api/defi/airdrops/refresh")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("refresh triggered") }
            }
    }
}
