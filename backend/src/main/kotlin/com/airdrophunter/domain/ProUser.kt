package com.airdrophunter.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

/**
 * Represents a paying PRO subscriber activated via Gumroad.
 * The [licenseKey] is the Gumroad product license key, used as the
 * activation credential in [com.airdrophunter.controller.AuthController].
 */
@Entity
@Table(name = "pro_users")
data class ProUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(name = "license_key", nullable = false, unique = true, length = 128)
    val licenseKey: String,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "activated_at", nullable = false)
    val activatedAt: OffsetDateTime = OffsetDateTime.now(),

    /** Gumroad sale_id for webhook de-duplication */
    @Column(name = "gumroad_sale_id", length = 128)
    val gumroadSaleId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
