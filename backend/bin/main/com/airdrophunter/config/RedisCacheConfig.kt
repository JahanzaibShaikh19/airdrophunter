package com.airdrophunter.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

@Configuration
class RedisCacheConfig {

    @Bean
    fun redisCacheManager(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisCacheManager {
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)
        val configuration = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(configuration)
            .transactionAware()
            .build()
    }
}
