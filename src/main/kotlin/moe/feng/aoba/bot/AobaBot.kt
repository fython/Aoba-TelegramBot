package moe.feng.aoba.bot

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.bot.functions.*
import moe.feng.aoba.event.DeliverBombGame
import moe.feng.aoba.event.GuessNumberGame
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.api.objects.Chat

class AobaBot : BaseTelegramBot(BotKeystore.botKey) {

	init {
		registerHelpFunctions()
		registerTextSpaceFunctions()
		registerReplaceFunctions()

		registerGameOptions()
		registerGame<DeliverBombGame>(
				"/bomb_game",
				resources["BOMB_GAME_IS_PLAYING"],
				{ message -> DeliverBombGame(message.chatId, this).apply { participants += message.from } }
		)
		registerGame<GuessNumberGame>(
				"/guess_number_game",
				resources["GUESS_NUMBER_GAME_IS_PLAYING"],
				{ message ->  GuessNumberGame(message.chatId, this).apply { participants += message.from } }
		)
	}

	override fun isAllowedBeUsed(chat: Chat): Boolean = true

	override fun isAllowedReceiveOldMessage(): Boolean = false

	companion object {

		val resources by resourceBundle("aoba_bot")

	}

}
