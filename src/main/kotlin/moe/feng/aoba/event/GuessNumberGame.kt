package moe.feng.aoba.event

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.support.LocalProperties.BotInfo
import moe.feng.aoba.res.Stickers
import moe.feng.aoba.support.get
import moe.feng.aoba.support.limitIn
import moe.feng.aoba.support.nextInt
import moe.feng.aoba.support.resourceBundle
import moe.feng.common.kt.StringUtil
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class GuessNumberGame(chatId: Long, bot: BaseTelegramBot) : BaseGame(chatId, bot) {

	// 召集信息
	private var collectMessage: Message? = null
	private lateinit var joinButton: InlineKeyboardButton
	private lateinit var startButton: InlineKeyboardButton
	private val collectMarkupInline: InlineKeyboardMarkup = InlineKeyboardMarkup()

	private var min = 0
	private var max = 0
	private var correct = 0
	private var initialMax: Int? = null

	private val messageTextGamePrepare: String get() = resources["GAME_PREPARE"].format(
			StringUtil.toMarkdownSafe(currentPlayer.getDisplayName()),
			makeParticipantsIdList(),
			MIN_RANGE,
			MAX_RANGE
	)

	override fun onStart() {
		joinButton = createInlineKeyboardButton(data = "join_guess_number") {
			onJoinRequest(it)
		}
		startButton = createInlineKeyboardButton(text = baseResources["GAME_START_BUTTON"], data = "start_guess_number") {
			onStartRequest(it)
		}
		collectMarkupInline.keyboard = mutableListOf(mutableListOf(joinButton), mutableListOf(startButton))
		// 发送召集信息
		bot.sendSticker(chatId, stickerId = Stickers.konataShoot.fileId)
		collectMessage = bot.sendMessage(chatId) {
			text = messageTextGamePrepare
			joinButton.text = baseResources["GAME_JOIN"].format(participants.size)
			replyMarkup = collectMarkupInline
			enableMarkdown(true)
		}
	}

	override fun onGameStart() {
		StatisticsDao.guessNumberGame++
		// 发送开始通知
		bot.sendMessage(chatId) {
			text = resources["GAME_START"].format(participants.size, currentPlayer.toMentionText())
			enableMarkdown(true)
		}
		printCurrentTurn()
	}

	override fun onGameInterrupted() {
		bot.replyMessage(collectMessage) {
			text = resources["GAME_INTERRUPTED"]
		}
	}

	override fun onGameOver() {
		// 游戏结束判定输家
		bot.sendSticker(chatId, stickerId = Stickers.killCat.fileId)
		bot.sendMessage(chatId) {
			text = resources["GAME_OVER"].format(
					StringUtil.toMarkdownSafe(currentPlayer.getDisplayName()), currentPlayer.toMentionText())
		}
		// 禁言套餐
		/*RestrictChatMember().apply {
			chatId = this@GuessNumberGame.chatId.toString()
			userId = currentChooser.id
			canSendMessages = false
			untilDate = (System.currentTimeMillis() / 1000L).toInt() + 60
		}.let(bot::execute)*/
		stop()
	}

	override fun onStop() {
		super.onStop()
		try {
			bot.editMessageText(collectMessage!!) {
				text = messageTextGamePrepare
				replyMarkup = InlineKeyboardMarkup()
				enableMarkdown(true)
			}
		} catch (e : TelegramApiException) {

		}
	}

	override suspend fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return when (command) {
			// 设定最大范围命令
			"/guess_number_game_set" -> {
				onSetMaxRequest(args, message)
				true
			}
			"/guess_number_game_set@${BotInfo.username}" -> {
				onSetMaxRequest(args, message)
				true
			}
			// 接受游戏停止命令
			"/guess_number_game_stop" -> {
				bot.stopEvent<GuessNumberGame>(chatId)
				true
			}
			"/guess_number_game_stop@${BotInfo.username}" -> {
				bot.stopEvent<GuessNumberGame>(chatId)
				true
			}
			else -> false
		}
	}

	private fun onSetMaxRequest(args: List<String>, message: Message) {
		if (args.size == 1) {
			val newMax = args.firstOrNull()?.toIntOrNull()
			if (newMax != null && newMax >= MIN_RANGE && newMax <= MAX_RANGE) {
				initialMax = newMax
				bot.replyMessage(message) {
					text = resources["GAME_SET_MAX_SUCCESS"].format(newMax)
				}
				return
			}
		}
		bot.replyMessage(message) {
			text = resources["GAME_SET_MAX_FAILED"].format(MIN_RANGE, MAX_RANGE)
		}
	}

	private fun onStartRequest(callbackQuery: CallbackQuery): Boolean {
		if (callbackQuery.message.messageId != collectMessage?.messageId || callbackQuery.message.chatId != chatId) {
			return false
		}

		if (isPlaying()) {
			bot.answerCallbackQuery(callbackQuery) {
				showAlert = true
				text = resources["GAME_ALREADY_START"]
			}
		} else {
			if (participants.size <= 1) {
				bot.answerCallbackQuery(callbackQuery) {
					showAlert = true
					text = resources["GAME_NEED_MORE_PARTICIPANTS"]
				}
			} else {
				min = 0
				max = (initialMax ?: (100 + participants.size * 100)).limitIn(MIN_RANGE..MAX_RANGE)
				correct = random.nextInt(1 until max)
				bot.editMessageText(collectMessage!!) {
					text = messageTextGamePrepare
					replyMarkup = InlineKeyboardMarkup()
					enableMarkdown(true)
				}
				startGame()
			}
		}
		return true
	}

	private fun onJoinRequest(callbackQuery: CallbackQuery): Boolean {
		if (!isPlaying()
				&& callbackQuery.message.messageId == collectMessage?.messageId
				&& callbackQuery.message.chatId == chatId) {
			// 响应新玩家加入
			val added = findParticipant(callbackQuery.from) != null
			if (!added) {
				participants += callbackQuery.from
			}
			try {
				bot.answerCallbackQuery(callbackQuery) {
					this.showAlert = added
					this.text = if (!added) resources["GAME_JOIN_SUCCESS"] else resources["GAME_JOIN_FAILED_EXIST"]
				}
				// 更新召集消息
				if (!added) {
					bot.editMessageText(collectMessage!!) {
						text = messageTextGamePrepare
						joinButton.text = baseResources["GAME_JOIN"].format(participants.size)
						replyMarkup = collectMarkupInline
						enableMarkdown(true)
					}
				}
			} catch (e: TelegramApiException) {
				e.printStackTrace()
			}
			return true
		} else {
			return false
		}
	}

	private fun printCurrentTurn() {
		bot.sendMessage(chatId.toString()) {
			text = resources["GUESS_TURN_TEXT"].format(currentPlayer.toMentionText(), min, max)
			enableMarkdown(true)
		}
	}

	override suspend fun onTextReceived(message: Message): Boolean {
		if (isPlaying() && currentPlayer.id == message.from.id) {
			val number = message.text.toIntOrNull()
			if (number != null) {
				if (number >= max || number <= min) {
					bot.replyMessage(message) {
						text = resources["GUESS_TURN_WRONG_GUESS"]
					}
				} else {
					if (number == correct) {
						stopGame()
					} else {
						if (number > correct) {
							max = number
							bot.replyMessage(message) {
								text = resources["GUESS_TURN_BIGGER_THAN"].format(number)
							}
						} else if (number < correct) {
							min = number
							bot.replyMessage(message) {
								text = resources["GUESS_TURN_SMALLER_THAN"].format(number)
							}
						}
						currentPlayerIndex += 1
						currentPlayerIndex %= participants.size
						printCurrentTurn()
					}
				}
				return true
			}
		}
		return super.onTextReceived(message)
	}

	companion object {

		private val resources by resourceBundle("guess_number_game")

		private const val MIN_RANGE = 50
		private const val MAX_RANGE = 100000

	}

}
