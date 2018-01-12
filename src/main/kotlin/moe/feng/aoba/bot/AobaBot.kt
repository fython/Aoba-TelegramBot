package moe.feng.aoba.bot

import moe.feng.aoba.bot.common.BaseTelegramBot
import moe.feng.aoba.bot.common.sendMessage
import moe.feng.aoba.event.DeliverBombGame
import moe.feng.aoba.event.GuessNumberGame
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle
import moe.feng.aoba.support.toJson
import org.telegram.telegrambots.api.objects.Chat
import org.telegram.telegrambots.api.objects.Message

class AobaBot : BaseTelegramBot(BotKeystore.botKey) {

	init {
		listenCommand("/start") { _, message ->
			sendMessage(message.chatId.toString()) {
				text = """
					欢迎使用烧饼的 Bot~
					测试中……
					""".trimIndent()
			}
			true
		}
		listenCommand("/bomb_game") { _, message ->
			val event = findEvent<DeliverBombGame>(message.chatId)
			if (event?.isAlive != true) {
				startEvent(message.chatId, DeliverBombGame(message.chatId, this).apply {
					participants += message.from
				})
			} else {
				sendMessage(message.chatId.toString()) {
					text = resources["BOMB_GAME_IS_PLAYING"]
					replyToMessageId = message.messageId
				}
			}
			true
		}
		listenCommand("/guess_number_game") { _, message ->
			val event = findEvent<GuessNumberGame>(message.chatId)
			if (event?.isAlive != true) {
				startEvent(message.chatId, GuessNumberGame(message.chatId, this).apply {
					participants += message.from
				})
			} else {
				sendMessage(message.chatId.toString()) {
					text = resources["GUESS_NUMBER_GAME_IS_PLAYING"]
					replyToMessageId = message.messageId
				}
			}
			true
		}
	}

	override fun isAllowedBeUsed(chat: Chat): Boolean = true

	override fun isAllowedReceiveOldMessage(): Boolean = false

	companion object {

		private val resources by resourceBundle("aoba_bot")

	}

}
