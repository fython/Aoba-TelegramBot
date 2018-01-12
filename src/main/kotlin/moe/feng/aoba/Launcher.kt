package moe.feng.aoba

import moe.feng.aoba.bot.AobaBot
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.TelegramBotsApi

object Launcher {

	private val aobaBot by lazy { AobaBot() }

	@JvmStatic
	fun main(args: Array<String>) {
		ApiContextInitializer.init()
		val botsApi = TelegramBotsApi()

		botsApi.registerBot(aobaBot)
	}

}