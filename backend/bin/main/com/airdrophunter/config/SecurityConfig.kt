package com.airdrophunter.config

import com.airdrophunter.security.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security configuration.
 *
 * Route protection strategy:
 *  - All **public** GET endpoints (stats, free airdrop list, etc.) are open.
 *  - POST /api/auth/activate and POST /api/webhook/gumroad are open.
 *  - /api/airdrops/pro and /api/alerts require ROLE_PRO.
 *  - Actuator health is open; all other actuator paths are restricted.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // ── Public endpoints ───────────────────────────────────
                    .requestMatchers(HttpMethod.POST, "/api/auth/activate").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/webhook/gumroad").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/airdrops").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/airdrops/hot").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/stats").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/wallet/check").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/wallet/check").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/defi/airdrops").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/defi/airdrops/hot").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/defi/airdrops/soon").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/defi/airdrops/category/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()

                    // ── PRO-only endpoints ────────────────────────────────
                    .requestMatchers("/api/airdrops/pro").hasAuthority("ROLE_PRO")
                    .requestMatchers("/api/alerts/**").hasAuthority("ROLE_PRO")

                    // Everything else requires authentication
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
