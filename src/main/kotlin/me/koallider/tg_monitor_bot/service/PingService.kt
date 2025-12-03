package me.koallider.tg_monitor_bot.service

import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

@Service
class PingService {
    
    fun checkUrl(urlString: String): Pair<Boolean, String> {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.instanceFollowRedirects = true
            
            val responseCode = connection.responseCode
            val isAvailable = responseCode == 200
            
            val status = if (isAvailable) {
                "up"
            } else {
                "down (HTTP $responseCode)"
            }
            
            connection.disconnect()
            Pair(isAvailable, status)
        } catch (e: Exception) {
            Pair(false, "down ($e - ${e.message})")
        }
    }
}