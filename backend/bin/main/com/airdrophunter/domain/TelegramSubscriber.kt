package com.airdrophunter.domain

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "telegram_subscribers")
data class TelegramSubscriber(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "chat_id", nullable = false, unique = true)
    val chatId: Long,

    @Column(name = "email", length = 255)
    val email: String? = null,

    @Column(name = "is_subscribed", nullable = false)
    val isSubscribed: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC)
)
