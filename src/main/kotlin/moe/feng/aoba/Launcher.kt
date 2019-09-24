package moe.feng.aoba

import moe.feng.aoba.api.RemoteYeelightApi
import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.res.BotOptions
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.TelegramBotsApi

object Launcher {

	private val aobaBot by lazy {
		val botOptions = DefaultBotOptions()
		if (BotOptions.useProxy) {
			botOptions.proxyType = DefaultBotOptions.ProxyType.HTTP
			botOptions.proxyHost = BotOptions.httpProxyHost
			botOptions.proxyPort = BotOptions.httpProxyPort
		}
		return@lazy AobaBot(botOptions)
	}

	@JvmStatic
	fun main(args: Array<String>) {
		ApiContextInitializer.init()
		val botsApi = TelegramBotsApi()

		RemoteYeelightApi.startListening()
		StatisticsDao.lastLaunchTime = System.currentTimeMillis()
		botsApi.registerBot(aobaBot)
	}

}