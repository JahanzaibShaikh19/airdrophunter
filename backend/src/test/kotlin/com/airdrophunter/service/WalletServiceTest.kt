package com.airdrophunter.service

import com.airdrophunter.client.AlchemyClient
import com.airdrophunter.client.EtherscanClient
import com.airdrophunter.client.ZkSyncClient
import com.airdrophunter.domain.Airdrop
import com.airdrophunter.dto.WalletCheckRequest
import com.airdrophunter.repository.AirdropRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("WalletService")
class WalletServiceTest {

    private val airdropRepository: AirdropRepository = mockk()
    private val etherscanClient: EtherscanClient = mockk()
    private val alchemyClient: AlchemyClient = mockk()
    private val zkSyncClient: ZkSyncClient = mockk()

    private lateinit var service: WalletService

    @BeforeEach
    fun setUp() {
        service = WalletService(airdropRepository, etherscanClient, alchemyClient, zkSyncClient)
    }

    @Test
    fun `returns eligible airdrops and total estimated value from on-chain signals`() {
        every { etherscanClient.getTransactionCount(any()) } returns 14L
        every { alchemyClient.getTokenBalanceCount(any()) } returns 3
        every { alchemyClient.getNftCount(any()) } returns 1
        every { zkSyncClient.getTransactionCount(any()) } returns 8L
        every { airdropRepository.findAllByIsActiveTrueOrderByEstimatedValueDesc() } returns listOf(
            airdrop(name = "zkSync Era Airdrop", protocol = "zkSync Era", chain = "Ethereum L2", value = BigDecimal("1800.00")),
            airdrop(name = "LayerZero Airdrop", protocol = "LayerZero", chain = "Multi-chain", value = BigDecimal("2500.00")),
            airdrop(name = "EigenLayer Restaking", protocol = "EigenLayer", chain = "Ethereum", value = BigDecimal("3100.00"))
        )

        val result = service.checkWallet(WalletCheckRequest("  0xD8DA6BF26964AF9D7EED9E03E53415D37AA96045  "))

        assertEquals("0xd8da6bf26964af9d7eed9e03e53415d37aa96045", result.address)
        assertEquals(3, result.eligibleAirdrops.size)
        assertEquals("$7400.00", result.totalEstimatedValue)
        assertTrue(result.eligibleAirdrops.any { it.airdropName == "zkSync Era Airdrop" })
    }

    @Test
    fun `rejects invalid ethereum addresses`() {
        val exception = org.junit.jupiter.api.assertThrows<ResponseStatusException> {
            service.checkWallet(WalletCheckRequest("not-a-wallet"))
        }

        assertEquals(HttpStatus.BAD_REQUEST, exception.statusCode)
        assertTrue(exception.reason!!.contains("valid Ethereum address"))
    }

    @Test
    fun `degrades gracefully when external providers fail`() {
        every { etherscanClient.getTransactionCount(any()) } throws IllegalStateException("Etherscan down")
        every { alchemyClient.getTokenBalanceCount(any()) } returns 0
        every { alchemyClient.getNftCount(any()) } throws IllegalStateException("Alchemy down")
        every { zkSyncClient.getTransactionCount(any()) } returns 0L
        every { airdropRepository.findAllByIsActiveTrueOrderByEstimatedValueDesc() } returns emptyList()

        val result = service.checkWallet(WalletCheckRequest("0xd8da6bf26964af9d7eed9e03e53415d37aa96045"))

        assertEquals("$0.00", result.totalEstimatedValue)
        assertTrue(result.eligibleAirdrops.isEmpty())
        assertTrue(result.recommendations.any { it.contains("Ethereum mainnet transaction count is temporarily unavailable") })
        assertTrue(result.recommendations.any { it.contains("Alchemy NFT lookup is temporarily unavailable") })
    }

    private fun airdrop(
        name: String,
        protocol: String,
        chain: String,
        value: BigDecimal
    ) = Airdrop(
        id = 1L,
        name = name,
        description = "Test airdrop",
        token = protocol.take(5).uppercase(),
        protocol = protocol,
        chain = chain,
        estimatedValue = value,
        endsAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5),
        isActive = true,
        websiteUrl = "https://example.com",
        createdAt = OffsetDateTime.now(ZoneOffset.UTC)
    )
}
