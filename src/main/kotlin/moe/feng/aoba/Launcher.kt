package moe.feng.aoba

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.dao.common.KVDatabase
import moe.feng.aoba.support.LocalProperties
import moe.feng.aoba.support.LocalProperties.Proxy
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi

object Launcher {

	private val aobaBot by lazy {
		val botOptions = DefaultBotOptions()
		if (Proxy.enable) {
			botOptions.proxyType = when (Proxy.type) {
				Proxy.TYPE_HTTP -> DefaultBotOptions.ProxyType.HTTP
				Proxy.TYPE_SOCKS4 -> DefaultBotOptions.ProxyType.SOCKS4
				Proxy.TYPE_SOCKS5 -> DefaultBotOptions.ProxyType.SOCKS5
				else -> throw IllegalArgumentException(
						"Unsupported proxy type: ${Proxy.type}")
			}
			botOptions.proxyHost = Proxy.host
			botOptions.proxyPort = Proxy.port
		}
		return@lazy AobaBot(botOptions)
	}

	@JvmStatic
	fun main(args: Array<String>) {
		var configPath = "aoba-bot.cfg"
		if (args.size == 1) {
			configPath = args[0]
		}
		LocalProperties.init(configPath)
		KVDatabase.init()

		ApiContextInitializer.init()
		val botsApi = TelegramBotsApi()
		StatisticsDao.lastLaunchTime = System.currentTimeMillis()
		botsApi.registerBot(aobaBot)
	}

}
