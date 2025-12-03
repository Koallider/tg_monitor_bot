package me.koallider.tg_monitor_bot.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "monitored_urls")
data class MonitoredUrl(
    val id: Int,
    val url: String,
    var status: String = "unknown", // "up", "down", "unknown"
    var lastChecked: Long = System.currentTimeMillis()
)

@Document(collection = "user_chats")
data class TodoChat(
    @Id val chatId: Long,
    var monitoredUrls: MutableList<MonitoredUrl> = mutableListOf(),
    var nextId: Int = 1
)