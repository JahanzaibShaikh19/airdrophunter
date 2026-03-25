package com.airdrophunter.repository

import com.airdrophunter.domain.TelegramSubscriber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TelegramSubscriberRepository : JpaRepository<TelegramSubscriber, Long> {
    fun findByChatId(chatId: Long): TelegramSubscriber?
    fun findAllByIsSubscribedTrue(): List<TelegramSubscriber>
}
