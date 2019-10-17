package moe.feng.aoba.support

import moe.feng.aoba.bot.common.BaseTelegramBot
import java.io.File
import java.lang.Exception
import java.util.*

object LocalProperties {

    private val properties: Properties = Properties()

    object Proxy {

        const val TYPE_HTTP = "http"
        const val TYPE_SOCKS4 = "socks4"
        const val TYPE_SOCKS5 = "socks5"

        var enable: Boolean = false
            internal set
        var type: String = TYPE_HTTP
            internal set
        var host: String = "127.0.0.1"
            internal set
        var port: Int = 1080
            internal set

    }

    object BotInfo {

        var token: String = ""
            internal set
        var username: String = ""
            internal set

        fun getBotKey(): BaseTelegramBot.BotKey {
            return BaseTelegramBot.BotKey(token, username)
        }

    }

    fun init(configPath: String) {
        val configFile = File(configPath)
        if (configFile.isFile) {
            try {
                println("LocalProperties: load configuration from $configFile")
                configFile.inputStream().use {
                    properties.load(it)
                }
                Proxy.enable = properties.getProperty("USE_PROXY", "0") == "1"
                Proxy.type = properties.getProperty("PROXY_TYPE", Proxy.TYPE_HTTP)
                Proxy.host = properties.getProperty("PROXY_HOST", "127.0.0.1")
                Proxy.port = properties.getProperty("PROXY_PORT")?.toIntOrNull() ?: 1080
                BotInfo.token = properties.getProperty("BOT_TOKEN") ?: ""
                BotInfo.username = properties.getProperty("BOT_USERNAME") ?: ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
