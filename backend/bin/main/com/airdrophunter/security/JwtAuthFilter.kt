package com.airdrophunter.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Intercepts every request, extracts a Bearer JWT from the Authorization header,
 * validates it via [JwtService], and populates the [SecurityContextHolder] with a
 * [UsernamePasswordAuthenticationToken] carrying the user's granted authorities.
 *
 * Unauthenticated requests are passed through; Spring Security's access rules
 * then decide whether to reject them.
 */
@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); return
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        if (!jwtService.isValid(token)) {
            if (jwtService.isExpired(token)) {
                log.debug("Expired JWT for request to ${request.requestURI}")
            }
            filterChain.doFilter(request, response); return
        }

        val email = jwtService.extractEmail(token)
        val role  = jwtService.extractRole(token) ?: JwtService.ROLE_FREE

        if (email != null && SecurityContextHolder.getContext().authentication == null) {
            val auth = UsernamePasswordAuthenticationToken(
                email,
                null,
                listOf(SimpleGrantedAuthority(role))
            )
            auth.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = auth
            log.debug("JWT auth set for $email with role $role")
        }

        filterChain.doFilter(request, response)
    }
}
