package moe.feng.aoba.bot

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.dao.GroupRulesDao
import moe.feng.aoba.event.BaseGame
import moe.feng.aoba.event.DeliverBombGame
import moe.feng.aoba.event.GuessNumberGame
import moe.feng.aoba.model.GroupRules
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.support.get
import moe.feng.aoba.support.isChinese
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.api.objects.Chat
import org.telegram.telegrambots.api.objects.Message

class AobaBot : BaseTelegramBot(BotKeystore.botKey) {

	init {
		listenCommand("/start") { _, message ->
			if (message.chat.isUserChat) {
				sendMessage(message.chatId.toString()) {
					text = """
					欢迎使用烧饼的 Bot~
					本机器人由 @fython 进行开发维护，如有建议可向作者反馈。使用说明请输入 /help 命令。
					代码在 GPL v3 协议下公开：https://github.com/fython/Aoba-TelegramBot
					""".trimIndent()
				}
			}
			true
		}
		listenCommand("/help") { _, message ->
			replyMessage(message) {
				text = if (message.chat.isUserChat) {
					"""
					烧饼 Bot 使用说明~

					/replace - 替换一句话中的关键词为另一个词
					/add_space - 每个汉字都使用空格隔开
					/remove_space - 去掉汉字间的空格

					已支持的游戏（规则请调用命令后查看）：
					/bomb_game - 开始传炸弹游戏
					/guess_number_game - 开始猜数字游戏
					""".trimIndent()
				} else {
					"群组内帮助已关闭，请私聊机器人咨询。"
				}
			}
			true
		}
		listenCommand("/add_space") { _, message ->
			if (message.isReply && message.replyToMessage.hasText()) {
				var newText = message.replyToMessage.text.trim()
				for ((i, c) in newText.withIndex().reversed()) {
					if (c.isChinese() || newText.getOrElse(i - 1, {'_'}).isChinese()) {
						newText = newText.substring(0, i) + " " + newText.substring(i, newText.length)
					}
				}
				sendMessage(message.chatId.toString()) {
					text = message.replyToMessage.from.getDisplayName() + ": " + newText
				}
			} else {
				replyMessage(message) {
					text = "请选择一条文本信息进行回复。"
				}
			}
			true
		}
		listenCommand("/remove_space") { _, message ->
			if (message.isReply && message.replyToMessage.hasText()) {
				var newText = message.replyToMessage.text.trim()
				for ((i, c) in newText.withIndex().reversed()) {
					if (c == ' '
							&& newText.getOrElse(i - 1, {'_'} ).isChinese()
							&& newText.getOrElse(i + 1, {'_'} ).isChinese()) {
						newText = newText.removeRange(i, i + 1)
					}
				}
				sendMessage(message.chatId.toString()) {
					text = message.replyToMessage.from.getDisplayName() + ": " + newText
				}
			} else {
				replyMessage(message) {
					text = "请选择一条文本信息进行回复。"
				}
			}
			true
		}
		listenCommand("/replace") { args, message ->
			if (message.isReply && message.replyToMessage.hasText()) {
				if (args.size == 2) {
					sendMessage(message.chatId.toString()) {
						text = resources["REPLACE_REPLY_RESULT"].format(
								message.from.getDisplayName(),
								if (message.from.id != message.replyToMessage.from.id) {
									message.replyToMessage.from.getDisplayName()
								} else {
									"Ta"
								},
								message.replyToMessage.text.replace(args[0], "<b>" + args[1] + "</b>")
						)
						enableHtml(true)
					}
				} else {
					replyMessage(message) {
						text = resources["REPLACE_REPLY_WRONG_ARGS"]
					}
				}
			} else {
				replyMessage(message) {
					text = if (args.isEmpty()) {
						resources["REPLACE_NEED_REPLY_TO_MESSAGE_NO_ARGS"]
					} else {
						resources["REPLACE_NEED_REPLY_TO_MESSAGE_HAVE_ARGS"]
					}
				}
			}
			true
		}

		listenCommand("/allow_game") { args, message ->
			if (message.chat.isGroupChat || message.chat.isSuperGroupChat) {
				when {
					args.isEmpty() -> {
						val isAllowGame = GroupRulesDao.data.find { it.id == message.chatId }?.isAllowGame ?: false
						replyMessage(message) {
							text = resources["ALLOW_GAME_STATUS"].format(
									resources[if (isAllowGame) "ALLOW" else "DISALLOW"]
							)
							enableMarkdown(true)
						}
					}
					args.size == 1 -> {
						if (hasAdminAccess(message.from, message.chatId)) {
							val value = when (args[0].toLowerCase()) { "true" -> true; "false" -> false; else -> null }
							if (value == null) {
								replyMessage(message) {
									text = resources["ALLOW_GAME_WRONG_ARGS"]
									enableMarkdown(true)
								}
							} else {
								val groupRules = GroupRulesDao.data.find { it.id == message.chatId }
										?: GroupRules(message.chatId).apply { GroupRulesDao.data.add(this) }
								groupRules.isAllowGame = value
								GroupRulesDao.scheduleSave()
								replyMessage(message) {
									text = resources["ALLOW_GAME_MODIFIED_STATUS"].format(
											resources[if (value) "ALLOW" else "DISALLOW"]
									)
								}
							}
						} else {
							replyMessage(message) { text = resources["COMMAND_NEED_ADMIN"] }
						}
					}
					else -> {
						replyMessage(message) {
							text = resources["ALLOW_GAME_WRONG_ARGS"]
							enableMarkdown(true)
						}
					}
				}
			} else {
				replyMessage(message) { text = resources["COMMAND_NEED_GROUP"] }
			}
			true
		}
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

	private inline fun <reified T : BaseGame> registerGame(
			openCommand: String,
			playingWarningText: String,
			crossinline gameCreator: (message: Message) -> BaseGame
	) {
		listenCommand(openCommand) { _, message ->
			val event = findEvent<T>(message.chatId)
			if (event?.isAlive != true) {
				startEvent(message.chatId, gameCreator(message))
			} else {
				sendMessage(message.chatId.toString()) {
					text = playingWarningText
					replyToMessageId = message.messageId
				}
			}
			true
		}
	}

	companion object {

		private val resources by resourceBundle("aoba_bot")

	}

}
