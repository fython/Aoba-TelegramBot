package moe.feng.aoba.res

import moe.feng.aoba.bot.common.BaseTelegramBot
import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle

object BotKeystore {

	private val resources by resourceBundle("keystore")

	val botKey = BaseTelegramBot.BotKey(resources["BOT_TOKEN"], resources["BOT_USERNAME"])

}