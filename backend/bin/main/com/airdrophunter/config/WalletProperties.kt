package com.airdrophunter.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "wallet.apis")
data class WalletProperties(
    val etherscanUrl: String,
    val etherscanApiKey: String,
    val alchemyUrl: String,
    val alchemyApiKey: String,
    val zksyncUrl: String
)
