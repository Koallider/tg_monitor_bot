package me.koallider.tg_monitor_bot.service

import me.koallider.tg_monitor_bot.controller.MainController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.text.SimpleDateFormat
import java.util.*

@Service
class ScheduledMonitorService {
    
    @Autowired
    lateinit var monitorService: MonitorService
    
    @Autowired
    lateinit var pingService: PingService
    
    @Autowired
    lateinit var bot: MainController
    
    @Scheduled(fixedRate = 60000) // Every minute
    fun checkAllUrls() {
        val allChats = monitorService.getAllChats()
        
        allChats.forEach { chat ->
            chat.monitoredUrls.forEach { url ->
                val (isAvailable, status) = pingService.checkUrl(url.url)
                val previousStatus = url.status
                val wasUp = previousStatus == "up"
                
                // Update status
                val newStatus = if (isAvailable) "up" else status
                monitorService.updateUrlStatus(chat.chatId, url.id, newStatus)
                
                // Only notify on status changes
                if (wasUp && !isAvailable) {
                    // URL went from up to down
                    val messageText = "⚠️ URL is DOWN!\n" +
                            "ID: #${url.id}\n" +
                            "URL: ${url.url}\n" +
                            "Status: $status"
                    
                    sendMessage(chat.chatId, messageText)
                } else if (!wasUp && isAvailable) {
                    // URL came back up after being down
                    val messageText = "✅ URL is BACK UP!\n" +
                            "ID: #${url.id}\n" +
                            "URL: ${url.url}\n" +
                            "Status: up"
                    
                    sendMessage(chat.chatId, messageText)
                }
            }
        }
    }
    
    @Scheduled(cron = "0 0 20 * * ?") // Every day at 8:00 PM (20:00)
    fun sendDailyStatusReport() {
        val allChats = monitorService.getAllChats()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        
        allChats.forEach { chat ->
            if (chat.monitoredUrls.isEmpty()) {
                // Send a message even if no URLs are monitored to confirm bot is working
                val messageText = "📊 Daily Status Report - $currentTime\n\n" +
                        "No monitored URLs configured.\n" +
                        "Bot is running and monitoring service is active."
                
                sendMessage(chat.chatId, messageText)
                return@forEach
            }
            
            // Check all URLs and build status report
            val statusLines = mutableListOf<String>()
            var allUp = true
            
            chat.monitoredUrls.forEach { url ->
                val (isAvailable, status) = pingService.checkUrl(url.url)
                val statusString = if (isAvailable) "up" else status
                
                // Update status
                monitorService.updateUrlStatus(chat.chatId, url.id, statusString)
                
                val statusIcon = if (isAvailable) "✅" else "❌"
                statusLines.add("$statusIcon #${url.id} - ${url.url}")
                statusLines.add("   Status: $statusString")
                statusLines.add("")
                
                if (!isAvailable) {
                    allUp = false
                }
            }
            
            // Build the report message
            val reportHeader = if (allUp) {
                "📊 Daily Status Report - $currentTime\n\n" +
                "✅ All servers are ONLINE\n\n"
            } else {
                "📊 Daily Status Report - $currentTime\n\n" +
                "⚠️ Some servers are DOWN\n\n"
            }
            
            val messageText = reportHeader + statusLines.joinToString("\n")
            
            sendMessage(chat.chatId, messageText)
        }
    }
    
    private fun sendMessage(chatId: Long, text: String) {
        val sendMessage = SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
        try {
            bot.execute(sendMessage)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}
