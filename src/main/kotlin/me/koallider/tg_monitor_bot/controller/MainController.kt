package me.koallider.tg_monitor_bot.controller

import me.koallider.tg_monitor_bot.service.MonitorService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException


@Component
class MainController : TelegramLongPollingBot() {

    @Autowired
    lateinit var monitorService: MonitorService

    @Value("\${tg.apiKey}")
    private lateinit var token: String

    @Value("\${tg.chatId}")
    private lateinit var chatId: String

    override fun getBotToken(): String {
        return token
    }

    override fun getBotUsername(): String {
        return "MonitorBot"
    }

    override fun onUpdateReceived(update: Update) {
        if (update.hasMessage() && update.message.hasText()) {
            val message = update.message
            val chatId = message.chatId
            val text = message.text

            if (this.chatId.toLong() != chatId) {
                sendMessage(chatId, "You shall not pass")
                return
            }

            when {
                text.startsWith("/start") -> {
                    sendMessage(
                        chatId, "Welcome to Monitor Bot!\n\n" +
                                "Commands:\n" +
                                "/add {url} - Add a URL to monitor\n" +
                                "/remove # - Remove a URL by ID\n" +
                                "/list - List all monitored URLs with their status"
                    )
                }

                text.startsWith("/add") -> {
                    val url = text.removePrefix("/add").trim()
                    if (url.isEmpty()) {
                        sendMessage(chatId, "Usage: /add {url}\nExample: /add https://example.com")
                    } else {
                        val response = monitorService.handleAddMessage(message.chat, url)
                        sendMessage(chatId, response)
                    }
                }

                text.startsWith("/list") -> {
                    val response = monitorService.handleListMessage(message.chat)
                    sendMessage(chatId, response)
                }

                text.startsWith("/remove") -> {
                    val idString = text.removePrefix("/remove").trim().removePrefix("#")
                    if (idString.isEmpty()) {
                        sendMessage(chatId, "Usage: /remove #\nExample: /remove 1")
                    } else {
                        val response = monitorService.handleRemoveMessage(message.chat, idString)
                        sendMessage(chatId, response)
                    }
                }
            }
        }
    }

    private fun sendMessage(chatId: Long, text: String) {
        val sendMessage = SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
        try {
            execute(sendMessage)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}
