package me.koallider.tg_monitor_bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
open class TgTodoBotApplication{
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            runApplication<TgTodoBotApplication>(*args)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<TgTodoBotApplication>(*args)
}
