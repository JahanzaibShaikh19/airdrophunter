package com.airdrophunter.service

import com.airdrophunter.domain.ProUser
import com.airdrophunter.repository.ProUserRepository
import com.airdrophunter.security.JwtProperties
import com.airdrophunter.security.JwtService
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.Optional

@DisplayName("ProUserService")
class ProUserServiceTest {

    private val repo: ProUserRepository = mockk(relaxed = true)
    private val jwtService: JwtService  = mockk()
    private lateinit var service: ProUserService

    private fun proUser(
        id: Long = 1L,
        email: String = "user@example.com",
        licenseKey: String = "LK-TEST-001",
        isActive: Boolean = true,
        saleId: String? = "sale_001"
    ) = ProUser(id = id, email = email, licenseKey = licenseKey,
        isActive = isActive, activatedAt = OffsetDateTime.now(),
        gumroadSaleId = saleId)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        service = ProUserService(repo, jwtService)
    }

    // ── activateFromGumroad ───────────────────────────────────────────────────

    @Nested
    @DisplayName("activateFromGumroad()")
    inner class Activate {

        @Test
        fun `creates a new ProUser when email does not exist`() {
            every { repo.existsByGumroadSaleId(any()) } returns false
            every { repo.findByEmail(any()) } returns Optional.empty()
            every { repo.save(any()) } answers { firstArg() }

            service.activateFromGumroad("new@user.com", "LK-NEW", "sale_new")

            verify { repo.save(match { it.email == "new@user.com" && it.licenseKey == "LK-NEW" }) }
        }

        @Test
        fun `re-activates and updates an existing deactivated user`() {
            val existing = proUser(isActive = false, saleId = null)
            every { repo.existsByGumroadSaleId(any()) } returns false
            every { repo.findByEmail("user@example.com") } returns Optional.of(existing)
            every { repo.save(any()) } answers { firstArg() }

            service.activateFromGumroad("user@example.com", "LK-NEW", "sale_002")

            verify { repo.save(match { it.isActive && it.licenseKey == "LK-NEW" }) }
        }

        @Test
        fun `skips processing for a duplicate Gumroad sale ID`() {
            every { repo.existsByGumroadSaleId("sale_001") } returns true
            every { repo.findByEmail(any()) } returns Optional.of(proUser())

            service.activateFromGumroad("user@example.com", "LK", "sale_001")

            verify(exactly = 0) { repo.save(any()) }
        }

        @Test
        fun `sets isActive to true on re-activation`() {
            val inactive = proUser(isActive = false)
            every { repo.existsByGumroadSaleId(any()) } returns false
            every { repo.findByEmail(any()) } returns Optional.of(inactive)
            val saved = slot<ProUser>()
            every { repo.save(capture(saved)) } answers { firstArg() }

            service.activateFromGumroad("user@example.com", "LK-X", "sale_x")

            assertTrue(saved.captured.isActive)
        }
    }

    // ── activateAndIssueToken ─────────────────────────────────────────────────

    @Nested
    @DisplayName("activateAndIssueToken()")
    inner class IssueToken {

        @Test
        fun `returns JWT for valid email and matching license key`() {
            every { repo.findByEmail("user@example.com") } returns Optional.of(proUser())
            every { jwtService.generateToken("user@example.com", true) } returns "jwt.token.here"

            val token = service.activateAndIssueToken("user@example.com", "LK-TEST-001")

            assertEquals("jwt.token.here", token)
        }

        @Test
        fun `normalises email to lowercase before lookup`() {
            every { repo.findByEmail("user@example.com") } returns Optional.of(proUser())
            every { jwtService.generateToken(any(), any()) } returns "tok"

            service.activateAndIssueToken("USER@EXAMPLE.COM", "LK-TEST-001")

            verify { repo.findByEmail("user@example.com") }
        }

        @Test
        fun `throws IllegalArgumentException for unknown email`() {
            every { repo.findByEmail(any()) } returns Optional.empty()
            assertThrows(IllegalArgumentException::class.java) {
                service.activateAndIssueToken("ghost@x.com", "any")
            }
        }

        @Test
        fun `throws IllegalArgumentException when license key does not match`() {
            every { repo.findByEmail("user@example.com") } returns Optional.of(proUser())
            assertThrows(IllegalArgumentException::class.java) {
                service.activateAndIssueToken("user@example.com", "WRONG-KEY")
            }
        }

        @Test
        fun `throws IllegalArgumentException when user is inactive`() {
            every { repo.findByEmail("user@example.com") } returns Optional.of(proUser(isActive = false))
            assertThrows(IllegalArgumentException::class.java) {
                service.activateAndIssueToken("user@example.com", "LK-TEST-001")
            }
        }
    }

    // ── deactivate ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deactivate()")
    inner class Deactivate {

        @Test
        fun `sets isActive to false for an existing user`() {
            every { repo.findByEmail("user@example.com") } returns Optional.of(proUser(isActive = true))
            val saved = slot<ProUser>()
            every { repo.save(capture(saved)) } answers { firstArg() }

            service.deactivate("user@example.com")

            assertFalse(saved.captured.isActive)
        }

        @Test
        fun `does nothing when email is not found`() {
            every { repo.findByEmail(any()) } returns Optional.empty()
            service.deactivate("ghost@x.com")
            verify(exactly = 0) { repo.save(any()) }
        }

        @Test
        fun `normalises email to lowercase`() {
            every { repo.findByEmail("user@example.com") } returns Optional.of(proUser())
            every { repo.save(any()) } answers { firstArg() }

            service.deactivate("USER@EXAMPLE.COM")

            verify { repo.findByEmail("user@example.com") }
        }
    }

    // ── isProUser ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isProUser()")
    inner class IsProUser {

        @Test
        fun `returns true for an active pro user`() {
            every { repo.existsByEmailAndIsActiveTrue("user@example.com") } returns true
            assertTrue(service.isProUser("user@example.com"))
        }

        @Test
        fun `returns false when user does not exist`() {
            every { repo.existsByEmailAndIsActiveTrue(any()) } returns false
            assertFalse(service.isProUser("ghost@x.com"))
        }

        @Test
        fun `normalises email to lowercase before query`() {
            every { repo.existsByEmailAndIsActiveTrue("user@example.com") } returns true
            service.isProUser("USER@EXAMPLE.COM")
            verify { repo.existsByEmailAndIsActiveTrue("user@example.com") }
        }
    }
}
