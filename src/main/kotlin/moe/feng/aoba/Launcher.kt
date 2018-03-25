package moe.feng.aoba

import moe.feng.aoba.api.RemoteYeelightApi
import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.dao.StatisticsDao
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi

object Launcher {

	private val aobaBot by lazy { AobaBot() }

	@JvmStatic
	fun main(args: Array<String>) {
		ApiContextInitializer.init()
		val botsApi = TelegramBotsApi()

		RemoteYeelightApi.startListening()
		StatisticsDao.lastLaunchTime = System.currentTimeMillis()
		botsApi.registerBot(aobaBot)
	}

}