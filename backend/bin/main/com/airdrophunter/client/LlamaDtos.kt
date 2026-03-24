package com.airdrophunter.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DeFiLlama /protocols response item.
 * Only the fields we care about are mapped; all others are silently ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LlamaProtocol(
    val id: String? = null,
    val name: String = "",
    val symbol: String? = null,
    val logo: String? = null,
    val slug: String? = null,
    val category: String? = null,
    val chains: List<String> = emptyList(),
    val tvl: Double? = null,
    val chainTvls: Map<String, Double> = emptyMap(),

    /** Optional future-launch signals */
    @JsonProperty("listedAt")
    val listedAt: Long? = null,

    /** Some protocols have an "airdrop" field set explicitly */
    val airdrop: Boolean? = null,
)

/**
 * Wrapper for coins.llama.fi/prices response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LlamaPriceResponse(
    val coins: Map<String, CoinPrice> = emptyMap()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CoinPrice(
    val price: Double? = null,
    val decimals: Int? = null,
    val symbol: String? = null,
    val timestamp: Long? = null,
    val confidence: Double? = null,
)
