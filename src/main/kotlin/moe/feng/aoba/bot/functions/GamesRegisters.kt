package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.hasAdminAccess
import moe.feng.aoba.bot.common.isAllowGame
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.bot.common.sendMessage
import moe.feng.aoba.dao.GroupRulesDao
import moe.feng.aoba.event.BaseGame
import moe.feng.aoba.model.GroupRules
import moe.feng.aoba.support.get
import moe.feng.aoba.support.setOrAdd
import org.telegram.telegrambots.meta.api.objects.Message

fun AobaBot.registerGameOptions() {
	listenCommand("/allow_game") { args, message ->
		if (message.chat.isGroupChat || message.chat.isSuperGroupChat) {
			when {
				args.isEmpty() -> {
					val isAllowGame = message.chat.isAllowGame()
					replyMessage(message) {
						text = AobaBot.resources["ALLOW_GAME_STATUS"].format(
								AobaBot.resources[if (isAllowGame) "ALLOW" else "DISALLOW"]
						)
						enableMarkdown(true)
					}
				}
				args.size == 1 -> {
					if (hasAdminAccess(message.from, message.chatId)) {
						val value = when (args[0].toLowerCase()) { "true" -> true; "false" -> false; else -> null }
						if (value == null) {
							replyMessage(message) {
								text = AobaBot.resources["ALLOW_GAME_WRONG_ARGS"]
								enableMarkdown(true)
							}
						} else {
							GroupRulesDao.setAllowGame(message.chatId, value)
							replyMessage(message) {
								text = AobaBot.resources["ALLOW_GAME_MODIFIED_STATUS"].format(
										AobaBot.resources[if (value) "ALLOW" else "DISALLOW"]
								)
							}
						}
					} else {
						replyMessage(message) { text = AobaBot.resources["COMMAND_NEED_ADMIN"] }
					}
				}
				else -> {
					replyMessage(message) {
						text = AobaBot.resources["ALLOW_GAME_WRONG_ARGS"]
						enableMarkdown(true)
					}
				}
			}
		} else {
			replyMessage(message) { text = AobaBot.resources["COMMAND_NEED_GROUP"] }
		}
		true
	}
}

inline fun <reified T : BaseGame> AobaBot.registerGame(
		openCommand: String,
		playingWarningText: String,
		crossinline gameCreator: (message: Message) -> BaseGame
) {
	listenCommand(openCommand) { _, message ->
		if (message.isGroupMessage || message.isSuperGroupMessage) {
			if (message.chat.isAllowGame()) {
				val event = findEvent<T>(message.chatId)
				if (event?.isAlive != true) {
					startEvent(message.chatId, gameCreator(message))
				} else {
					sendMessage(message.chatId.toString()) {
						text = playingWarningText
						replyToMessageId = message.messageId
					}
				}
			} else {
				replyMessage(message) {
					text = AobaBot.resources["ALLOW_GAME_STATUS_WITHOUT_TIPS"]
				}
			}
		} else {
			replyMessage(message) { text = AobaBot.resources["COMMAND_NEED_GROUP"] }
		}
		true
	}
}
