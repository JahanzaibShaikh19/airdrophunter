package com.airdrophunter.service

import com.airdrophunter.client.AlchemyClient
import com.airdrophunter.client.EtherscanClient
import com.airdrophunter.client.ZkSyncClient
import com.airdrophunter.domain.Airdrop
import com.airdrophunter.dto.AirdropEligibility
import com.airdrophunter.dto.WalletCheckRequest
import com.airdrophunter.dto.WalletResult
import com.airdrophunter.repository.AirdropRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class WalletService(
    private val airdropRepository: AirdropRepository,
    private val etherscanClient: EtherscanClient,
    private val alchemyClient: AlchemyClient,
    private val zkSyncClient: ZkSyncClient
) {

    private val log = LoggerFactory.getLogger(WalletService::class.java)

    @Cacheable(cacheNames = ["wallet-results"], key = "#request.address.trim().toLowerCase()")
    fun checkWallet(request: WalletCheckRequest): WalletResult {
        val normalizedAddress = request.address.trim().lowercase()
        validateEthereumAddress(normalizedAddress)

        val issues = mutableListOf<String>()
        val activity = WalletActivitySnapshot(
            l1TransactionCount = fetchLong(issues, "Ethereum mainnet transaction count") {
                etherscanClient.getTransactionCount(normalizedAddress)
            },
            tokenBalanceCount = fetchInt(issues, "Alchemy token balance lookup") {
                alchemyClient.getTokenBalanceCount(normalizedAddress)
            },
            nftCount = fetchInt(issues, "Alchemy NFT lookup") {
                alchemyClient.getNftCount(normalizedAddress)
            },
            zkSyncTransactionCount = fetchLong(issues, "zkSync Era transaction count") {
                zkSyncClient.getTransactionCount(normalizedAddress)
            }
        )

        val eligibleAirdrops = airdropRepository.findAllByIsActiveTrueOrderByEstimatedValueDesc()
            .mapNotNull { evaluateEligibility(it, activity) }

        val recommendations = buildRecommendations(activity, eligibleAirdrops.isEmpty(), issues)
        val totalEstimatedValue = eligibleAirdrops.fold(BigDecimal.ZERO) { total, item ->
            total + item.estimatedValue
        }

        return WalletResult(
            address = normalizedAddress,
            eligibleAirdrops = eligibleAirdrops.map {
                AirdropEligibility(
                    airdropName = it.airdropName,
                    protocol = it.protocol,
                    estimatedValue = it.estimatedValue.toMoneyString(),
                    reason = it.reason
                )
            },
            totalEstimatedValue = totalEstimatedValue.toMoneyString(),
            recommendations = recommendations
        )
    }

    private fun validateEthereumAddress(address: String) {
        if (!ETHEREUM_ADDRESS_REGEX.matches(address)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Wallet address must be a valid Ethereum address"
            )
        }
    }

    private fun evaluateEligibility(
        airdrop: Airdrop,
        activity: WalletActivitySnapshot
    ): EligibleAirdrop? {
        val protocol = airdrop.protocol.lowercase()
        val chain = airdrop.chain.lowercase()
        val name = airdrop.name.lowercase()

        val reason = when {
            name.contains("zksync") || protocol.contains("zksync") -> {
                if (activity.zkSyncTransactionCount > 5) {
                    "Detected ${activity.zkSyncTransactionCount} zkSync Era transactions."
                } else {
                    null
                }
            }

            protocol.contains("layerzero") || protocol.contains("wormhole") -> {
                if (activity.l1TransactionCount >= 8 && (activity.tokenBalanceCount > 0 || activity.nftCount > 0)) {
                    "Strong cross-chain profile with ${activity.l1TransactionCount} L1 transactions and active token/NFT holdings."
                } else {
                    null
                }
            }

            protocol.contains("eigenlayer") -> {
                if (activity.l1TransactionCount >= 12 && activity.tokenBalanceCount > 0) {
                    "Restaking-style profile inferred from ${activity.l1TransactionCount} L1 transactions and ${activity.tokenBalanceCount} ERC-20 balances."
                } else {
                    null
                }
            }

            chain.contains("ethereum l2") -> {
                if (activity.l1TransactionCount >= 5 || activity.tokenBalanceCount >= 2 || activity.nftCount > 0) {
                    "Layer 2 eligibility inferred from Ethereum activity plus ${activity.tokenBalanceCount} token balances and ${activity.nftCount} NFTs."
                } else {
                    null
                }
            }

            chain.contains("ethereum") || chain.contains("multi-chain") -> {
                if (activity.l1TransactionCount >= 10 || activity.tokenBalanceCount >= 3) {
                    "Mainnet activity threshold met with ${activity.l1TransactionCount} transactions and ${activity.tokenBalanceCount} ERC-20 balances."
                } else {
                    null
                }
            }

            activity.tokenBalanceCount >= 1 -> {
                "Wallet holds on-chain assets that match the ${airdrop.protocol} participation profile."
            }

            else -> null
        }

        return reason?.let {
            EligibleAirdrop(
                airdropName = airdrop.name,
                protocol = airdrop.protocol,
                estimatedValue = airdrop.estimatedValue,
                reason = it
            )
        }
    }

    private fun buildRecommendations(
        activity: WalletActivitySnapshot,
        hasNoEligibleAirdrops: Boolean,
        issues: List<String>
    ): List<String> {
        val recommendations = linkedSetOf<String>()

        if (activity.l1TransactionCount < 10) {
            recommendations += "Increase Ethereum mainnet activity to at least 10 transactions to improve L1-based eligibility."
        }

        if (activity.zkSyncTransactionCount <= 5) {
            recommendations += "Use zkSync Era more frequently; more than 5 transactions unlock the zkSync rule in this checker."
        }

        if (activity.tokenBalanceCount == 0) {
            recommendations += "Hold or bridge ERC-20 assets before re-running the check so token-balance-based campaigns can match."
        }

        if (activity.nftCount == 0) {
            recommendations += "Mint or hold at least one NFT to improve community and NFT-gated airdrop coverage."
        }

        if (hasNoEligibleAirdrops) {
            recommendations += "No active airdrops matched the current wallet profile. Retry after more protocol activity or when new campaigns are added."
        }

        issues.forEach(recommendations::add)

        return recommendations.toList()
    }

    private fun fetchLong(
        issues: MutableList<String>,
        metricName: String,
        supplier: () -> Long
    ): Long = try {
        supplier()
    } catch (exception: Exception) {
        log.warn("Wallet lookup fallback for {}: {}", metricName, exception.message)
        issues += "$metricName is temporarily unavailable. Re-run the check in a few minutes for a fresher result."
        0L
    }

    private fun fetchInt(
        issues: MutableList<String>,
        metricName: String,
        supplier: () -> Int
    ): Int = try {
        supplier()
    } catch (exception: Exception) {
        log.warn("Wallet lookup fallback for {}: {}", metricName, exception.message)
        issues += "$metricName is temporarily unavailable. Re-run the check in a few minutes for a fresher result."
        0
    }

    private data class WalletActivitySnapshot(
        val l1TransactionCount: Long,
        val tokenBalanceCount: Int,
        val nftCount: Int,
        val zkSyncTransactionCount: Long
    )

    private data class EligibleAirdrop(
        val airdropName: String,
        val protocol: String,
        val estimatedValue: BigDecimal,
        val reason: String
    )

    companion object {
        private val ETHEREUM_ADDRESS_REGEX = Regex("^0x[a-fA-F0-9]{40}$")
    }
}

private fun BigDecimal.toMoneyString(): String =
    "$${setScale(2, RoundingMode.HALF_UP).toPlainString()}"
