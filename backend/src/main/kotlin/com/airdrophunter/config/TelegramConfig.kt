package com.airdrophunter.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
data class TelegramConfig(
    var username: String = "",
    var token: String = ""
)
