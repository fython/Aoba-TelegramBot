package moe.feng.aoba.bot

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.bot.functions.*
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.event.DeliverBombGame
import moe.feng.aoba.event.GuessNumberGame
import moe.feng.aoba.event.MinesweeperGame
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.res.Stickers
import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message

class AobaBot(private val botOptions: DefaultBotOptions = DefaultBotOptions())
	 : BaseTelegramBot(BotKeystore.botKey, botOptions) {

	init {
		registerHelpFunctions()
		registerTextSpaceFunctions()
		registerReplaceFunctions()
		registerHelpMeChooseFunction()
		registerStatisticsFunctions()
		registerYeelightFunctions()

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
		registerGame<MinesweeperGame>(
				"/minesweeper_game",
				resources["MINESWEEPER_GAME_IS_PLAYING"],
				{ message -> MinesweeperGame(message.chatId, this).apply { participants += message.from } }
		)

		listenSticker(Stickers.invertedQuestion) { msg ->
			sendSticker(msg.chatId.toString()) {
				replyToMessageId = msg.messageId
				setSticker(Stickers.munikoQuestion.fileId)
			}
			true
		}
	}

	override fun isAllowedBeUsed(chat: Chat): Boolean = true

	override fun isAllowedReceiveOldMessage(): Boolean = false

	override suspend fun onTextReceived(message: Message): Boolean {
		if (message.chat.isGroupChat || message.chat.isSuperGroupChat) {
			if (message.chatId !in StatisticsDao.joinedGroups) {
				StatisticsDao.joinedGroups.add(message.chatId)
				StatisticsDao.scheduleSave()
			}
		}
		return super.onTextReceived(message)
	}

	companion object {

		val resources by resourceBundle("aoba_bot")

	}

}
