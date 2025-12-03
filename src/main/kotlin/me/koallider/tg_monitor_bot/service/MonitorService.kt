package me.koallider.tg_monitor_bot.service

import org.telegram.telegrambots.meta.api.objects.Chat
import me.koallider.tg_monitor_bot.dao.ChatRepository
import me.koallider.tg_monitor_bot.model.MonitoredUrl
import me.koallider.tg_monitor_bot.model.TodoChat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


interface MonitorService {
    fun handleAddMessage(chat: Chat, url: String): String

    fun handleListMessage(chat: Chat): String

    fun handleRemoveMessage(chat: Chat, idString: String): String
    
    fun getAllChats(): List<TodoChat>
    
    fun updateUrlStatus(chatId: Long, urlId: Int, status: String)
}

@Service
class MonitorServiceImpl : MonitorService {
    @Autowired
    lateinit var chatRepository: ChatRepository
    
    @Autowired
    lateinit var pingService: PingService
    
    override fun handleAddMessage(chat: Chat, url: String): String {
        val urlText = url.trim()
        
        // Basic URL validation
        if (!urlText.startsWith("http://") && !urlText.startsWith("https://")) {
            return "Error: URL must start with http:// or https://"
        }
        
        try {
            val todoChat = chatRepository.findByChatId(chat.id) 
                ?: TodoChat(chatId = chat.id)
            
            // Check if URL already exists
            if (todoChat.monitoredUrls.any { it.url == urlText }) {
                return "Error: URL already monitored"
            }
            
            // Check URL immediately
            val (isAvailable, status) = pingService.checkUrl(urlText)
            val statusString = if (isAvailable) "up" else status
            
            val monitoredUrl = MonitoredUrl(
                id = todoChat.nextId++,
                url = urlText,
                status = statusString,
                lastChecked = System.currentTimeMillis()
            )
            
            todoChat.monitoredUrls.add(monitoredUrl)
            chatRepository.save(todoChat)
            
            return "Added URL #${monitoredUrl.id}: $urlText\nStatus: $statusString"
        } catch (e: Exception) {
            return "Error adding URL: ${e.message}"
        }
    }

    override fun handleListMessage(chat: Chat): String {
        val todoChat = chatRepository.findByChatId(chat.id)
        
        if (todoChat == null || todoChat.monitoredUrls.isEmpty()) {
            return "No monitored URLs. Use /add {url} to add one."
        }
        
        val lines = mutableListOf<String>()
        lines.add("Monitored URLs:")
        lines.add("")
        
        todoChat.monitoredUrls.forEach { url ->
            val lastCheckedDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(java.util.Date(url.lastChecked))
            lines.add("#${url.id} - ${url.url}")
            lines.add("  Status: ${url.status}")
            lines.add("  Last checked: $lastCheckedDate")
            lines.add("")
        }
        
        return lines.joinToString("\n")
    }

    override fun handleRemoveMessage(chat: Chat, idString: String): String {
        val idText = idString.trim().removePrefix("#")
        val id = idText.toIntOrNull()
        
        if (id == null) {
            return "Error: Invalid ID. Use a number like /remove 1"
        }
        
        val todoChat = chatRepository.findByChatId(chat.id)
            ?: return "Error: No monitored URLs found"
        
        val urlToRemove = todoChat.monitoredUrls.find { it.id == id }
        
        if (urlToRemove == null) {
            return "Error: URL with ID #$id not found"
        }
        
        todoChat.monitoredUrls.remove(urlToRemove)
        chatRepository.save(todoChat)
        
        return "Removed URL #$id: ${urlToRemove.url}"
    }
    
    override fun getAllChats(): List<TodoChat> {
        return chatRepository.findAll()
    }
    
    override fun updateUrlStatus(chatId: Long, urlId: Int, status: String) {
        val todoChat = chatRepository.findByChatId(chatId) ?: return
        val url = todoChat.monitoredUrls.find { it.id == urlId } ?: return
        
        url.status = status
        url.lastChecked = System.currentTimeMillis()
        
        chatRepository.save(todoChat)
    }
}