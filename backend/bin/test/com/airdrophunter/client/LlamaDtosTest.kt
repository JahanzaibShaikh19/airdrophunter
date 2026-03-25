package com.airdrophunter.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for DeFiLlama API response DTOs.
 * Uses a real ObjectMapper to verify Jackson deserialization behaviour,
 * including @JsonIgnoreProperties for forward-compatibility.
 */
@DisplayName("LlamaDtos — Jackson Deserialization")
class LlamaDtosTest {

    private lateinit var mapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        mapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    // ── LlamaProtocol ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("LlamaProtocol")
    inner class LlamaProtocolTests {

        @Test
        fun `deserialises a full protocol object`() {
            val json = """
                {
                  "id": "1",
                  "name": "LayerZero",
                  "symbol": "ZRO",
                  "logo": "https://cdn.llama.fi/layerzero.png",
                  "slug": "layerzero",
                  "category": "Bridge",
                  "chains": ["Ethereum", "Arbitrum", "Optimism"],
                  "tvl": 350000000.0,
                  "listedAt": 1700000000,
                  "airdrop": true
                }
            """.trimIndent()

            val proto: LlamaProtocol = mapper.readValue(json)

            assertEquals("1", proto.id)
            assertEquals("LayerZero", proto.name)
            assertEquals("ZRO", proto.symbol)
            assertEquals("https://cdn.llama.fi/layerzero.png", proto.logo)
            assertEquals("layerzero", proto.slug)
            assertEquals("Bridge", proto.category)
            assertEquals(listOf("Ethereum", "Arbitrum", "Optimism"), proto.chains)
            assertEquals(350_000_000.0, proto.tvl)
            assertEquals(1_700_000_000L, proto.listedAt)
            assertTrue(proto.airdrop == true)
        }

        @Test
        fun `uses default values when optional fields are absent`() {
            val json = """{"name": "MinimalProto"}"""
            val proto: LlamaProtocol = mapper.readValue(json)

            assertEquals("MinimalProto", proto.name)
            assertNull(proto.id)
            assertNull(proto.symbol)
            assertNull(proto.logo)
            assertNull(proto.slug)
            assertNull(proto.category)
            assertTrue(proto.chains.isEmpty())
            assertNull(proto.tvl)
            assertNull(proto.listedAt)
            assertNull(proto.airdrop)
        }

        @Test
        fun `ignores unknown fields without throwing`() {
            val json = """
                {
                  "name": "TestProto",
                  "symbol": "TST",
                  "unknownField": "should be ignored",
                  "anotherUnknown": 12345,
                  "deepUnknown": { "nested": true }
                }
            """.trimIndent()

            assertDoesNotThrow {
                val proto: LlamaProtocol = mapper.readValue(json)
                assertEquals("TestProto", proto.name)
            }
        }

        @Test
        fun `deserialises a list of protocols`() {
            val json = """
                [
                  {"name": "Proto A", "symbol": "A", "tvl": 1000000.0},
                  {"name": "Proto B", "symbol": "B", "tvl": 2000000.0},
                  {"name": "Proto C", "symbol": "C"}
                ]
            """.trimIndent()

            val protos: List<LlamaProtocol> = mapper.readValue(json)

            assertEquals(3, protos.size)
            assertEquals("Proto A", protos[0].name)
            assertEquals("B", protos[1].symbol)
            assertNull(protos[2].tvl)
        }

        @Test
        fun `handles null tvl without error`() {
            val json = """{"name": "NullTvl", "tvl": null}"""
            val proto: LlamaProtocol = mapper.readValue(json)
            assertNull(proto.tvl)
        }

        @Test
        fun `deserialises chainTvls map`() {
            val json = """
                {
                  "name": "Multi",
                  "chainTvls": {"Ethereum": 100000.0, "Arbitrum": 50000.0}
                }
            """.trimIndent()

            val proto: LlamaProtocol = mapper.readValue(json)

            assertEquals(2, proto.chainTvls.size)
            assertEquals(100_000.0, proto.chainTvls["Ethereum"])
            assertEquals(50_000.0,  proto.chainTvls["Arbitrum"])
        }

        @Test
        fun `maps listedAt epoch seconds correctly`() {
            val json = """{"name": "X", "listedAt": 1699999999}"""
            val proto: LlamaProtocol = mapper.readValue(json)
            assertEquals(1_699_999_999L, proto.listedAt)
        }
    }

    // ── CoinPrice ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CoinPrice")
    inner class CoinPriceTests {

        @Test
        fun `deserialises a CoinPrice object`() {
            val json = """
                {
                  "price": 2.45,
                  "decimals": 18,
                  "symbol": "ZRO",
                  "timestamp": 1700000000,
                  "confidence": 0.99
                }
            """.trimIndent()

            val coin: CoinPrice = mapper.readValue(json)

            assertEquals(2.45, coin.price)
            assertEquals(18, coin.decimals)
            assertEquals("ZRO", coin.symbol)
            assertEquals(1_700_000_000L, coin.timestamp)
            assertEquals(0.99, coin.confidence)
        }

        @Test
        fun `uses null defaults for all optional fields`() {
            val json = "{}"
            val coin: CoinPrice = mapper.readValue(json)
            assertNull(coin.price)
            assertNull(coin.decimals)
            assertNull(coin.symbol)
            assertNull(coin.timestamp)
            assertNull(coin.confidence)
        }

        @Test
        fun `ignores unknown CoinPrice fields`() {
            val json = """{"price": 1.0, "randomField": "ignored"}"""
            assertDoesNotThrow { val coin: CoinPrice = mapper.readValue(json); assertEquals(1.0, coin.price) }
        }
    }

    // ── LlamaPriceResponse ────────────────────────────────────────────────────

    @Nested
    @DisplayName("LlamaPriceResponse")
    inner class LlamaPriceResponseTests {

        @Test
        fun `deserialises coins map`() {
            val json = """
                {
                  "coins": {
                    "coingecko:zro": {"price": 2.45, "symbol": "ZRO", "confidence": 0.99},
                    "coingecko:eth": {"price": 3200.0, "symbol": "ETH", "confidence": 1.0}
                  }
                }
            """.trimIndent()

            val response: LlamaPriceResponse = mapper.readValue(json)

            assertEquals(2, response.coins.size)
            assertEquals(2.45,   response.coins["coingecko:zro"]?.price)
            assertEquals(3200.0, response.coins["coingecko:eth"]?.price)
            assertEquals("ZRO",  response.coins["coingecko:zro"]?.symbol)
        }

        @Test
        fun `returns empty coins map when field is absent`() {
            val json = "{}"
            val response: LlamaPriceResponse = mapper.readValue(json)
            assertTrue(response.coins.isEmpty())
        }

        @Test
        fun `handles empty coins object`() {
            val json = """{"coins": {}}"""
            val response: LlamaPriceResponse = mapper.readValue(json)
            assertTrue(response.coins.isEmpty())
        }

        @Test
        fun `coin prices with null price field are preserved`() {
            val json = """{"coins": {"coingecko:unknown": {"symbol": "UNK"}}}"""
            val response: LlamaPriceResponse = mapper.readValue(json)
            assertNull(response.coins["coingecko:unknown"]?.price)
        }
    }
}
