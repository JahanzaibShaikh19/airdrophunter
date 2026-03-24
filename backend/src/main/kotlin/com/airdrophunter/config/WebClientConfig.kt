package com.airdrophunter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    /**
     * Shared WebClient for DeFiLlama API calls.
     * Base URLs are set per-call so the same bean is reused for both
     * api.llama.fi and coins.llama.fi.
     */
    @Bean
    fun webClient(): WebClient = WebClient.builder()
        .defaultHeader("Accept", "application/json")
        .defaultHeader("User-Agent", "AirdropHunter/1.0")
        .codecs { it.defaultCodecs().maxInMemorySize(16 * 1024 * 1024) } // 16 MB
        .build()
}
