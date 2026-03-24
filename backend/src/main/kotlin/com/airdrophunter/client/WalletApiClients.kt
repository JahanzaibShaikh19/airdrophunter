package com.airdrophunter.client

import com.airdrophunter.config.WalletProperties
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.math.BigInteger

interface EtherscanClient {
    fun getTransactionCount(address: String): Long
}

interface AlchemyClient {
    fun getTokenBalanceCount(address: String): Int
    fun getNftCount(address: String): Int
}

interface ZkSyncClient {
    fun getTransactionCount(address: String): Long
}

@Component
class WebClientEtherscanClient(
    private val webClient: WebClient,
    private val walletProperties: WalletProperties
) : EtherscanClient {

    override fun getTransactionCount(address: String): Long {
        require(walletProperties.etherscanApiKey.isNotBlank()) {
            "ETHERSCAN_API_KEY is required for wallet checks"
        }

        val response = webClient.get()
            .uri(walletProperties.etherscanUrl) {
                it.queryParam("module", "proxy")
                    .queryParam("action", "eth_getTransactionCount")
                    .queryParam("address", address)
                    .queryParam("tag", "latest")
                    .queryParam("apikey", walletProperties.etherscanApiKey)
                    .build()
            }
            .retrieve()
            .bodyToMono<JsonNode>()
            .block()
            ?: error("Empty response from Etherscan")

        return decodeHexQuantity(response.path("result").asText())
    }
}

@Component
class WebClientAlchemyClient(
    private val webClient: WebClient,
    private val walletProperties: WalletProperties
) : AlchemyClient {

    private val log = LoggerFactory.getLogger(WebClientAlchemyClient::class.java)

    override fun getTokenBalanceCount(address: String): Int {
        val response = callAlchemy(
            method = "alchemy_getTokenBalances",
            params = listOf(address)
        )

        return response.path("result")
            .path("tokenBalances")
            .count { tokenNode -> tokenNode.path("tokenBalance").asText() != "0x0" }
    }

    override fun getNftCount(address: String): Int {
        val response = callAlchemy(
            method = "getNFTsForOwner",
            params = listOf(address, mapOf("pageSize" to 100))
        )

        return response.path("result")
            .path("ownedNfts")
            .size()
    }

    private fun callAlchemy(method: String, params: List<Any>): JsonNode {
        require(walletProperties.alchemyApiKey.isNotBlank()) {
            "ALCHEMY_API_KEY is required for wallet checks"
        }

        val requestBody = mapOf(
            "jsonrpc" to "2.0",
            "id" to method,
            "method" to method,
            "params" to params
        )

        val response = webClient.post()
            .uri("${walletProperties.alchemyUrl}/${walletProperties.alchemyApiKey}")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono<JsonNode>()
            .block()
            ?: error("Empty response from Alchemy")

        if (response.hasNonNull("error")) {
            log.warn("Alchemy returned error payload for {}: {}", method, response.path("error").toPrettyString())
            error("Alchemy request failed for $method")
        }

        return response
    }
}

@Component
class WebClientZkSyncClient(
    private val webClient: WebClient,
    private val walletProperties: WalletProperties
) : ZkSyncClient {

    override fun getTransactionCount(address: String): Long {
        val requestBody = mapOf(
            "jsonrpc" to "2.0",
            "id" to "zksync-tx-count",
            "method" to "eth_getTransactionCount",
            "params" to listOf(address, "latest")
        )

        val response = webClient.post()
            .uri(walletProperties.zksyncUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono<JsonNode>()
            .block()
            ?: error("Empty response from zkSync")

        return decodeHexQuantity(response.path("result").asText())
    }
}

private fun decodeHexQuantity(value: String): Long {
    val normalized = value.removePrefix("0x").ifBlank { "0" }
    return normalized.toBigInteger(16).coerceAtMost(BigInteger.valueOf(Long.MAX_VALUE)).toLong()
}

private fun JsonNode.count(predicate: (JsonNode) -> Boolean): Int {
    var total = 0
    elements().forEachRemaining { node ->
        if (predicate(node)) {
            total += 1
        }
    }
    return total
}
