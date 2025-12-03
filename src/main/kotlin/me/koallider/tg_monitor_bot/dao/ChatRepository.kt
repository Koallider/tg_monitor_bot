package me.koallider.tg_monitor_bot.dao

import me.koallider.tg_monitor_bot.model.TodoChat
import org.springframework.data.mongodb.repository.MongoRepository

interface ChatRepository : MongoRepository<TodoChat, Long> {
    fun findByChatId(chatId: Long): TodoChat?
}