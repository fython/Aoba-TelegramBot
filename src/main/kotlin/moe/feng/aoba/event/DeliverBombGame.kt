package moe.feng.aoba.event

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.res.Stickers
import moe.feng.aoba.support.get
import moe.feng.aoba.support.nextInt
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.*

class DeliverBombGame(chatId: Long, bot: BaseTelegramBot) : BaseGame(chatId, bot) {

	// 召集信息
	private var collectMessage: Message? = null
	private val collectMarkupInline = InlineKeyboardMarkup()
	private lateinit var startButton: InlineKeyboardButton
	private lateinit var joinButton: InlineKeyboardButton

	// 时间计数器
	private var currentTime = 0
	private val timer = Timer()

	private val messageTextGamePrepare: String get() = resources["GAME_PREPARE"].format(
			currentPlayer.getDisplayName(),
			makeParticipantsIdList()
	)

	override fun onStart() {
		joinButton = createInlineKeyboardButton(data = "join_deliver_bomb") {
			onJoinRequest(it)
		}
		startButton = createInlineKeyboardButton(text = baseResources["GAME_START_BUTTON"], data = "start_deliver_bomb") {
			onStartRequest(it)
		}
		collectMarkupInline.keyboard = mutableListOf(mutableListOf(joinButton), mutableListOf(startButton))
		// 发送召集信息
		bot.sendSticker(chatId.toString(), stickerId = Stickers.catWithClock.fileId)
		collectMessage = bot.sendMessage(chatId.toString()) {
			text = messageTextGamePrepare
			joinButton.text = baseResources["GAME_JOIN"].format(participants.size)
			replyMarkup = collectMarkupInline
			enableMarkdown(true)
		}
	}

	override fun onGameStart() {
		StatisticsDao.bombGame++
		// 发送开始通知
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_START"].format(participants.size, currentPlayer.toMentionText(), MAX_GAME_TIME)
			enableMarkdown(true)
		}
		// 设定定时器
		timer.scheduleAtFixedRate(TickTock(), 0, 1000)
	}

	override fun onGameOver() {
		timer.cancel()
		// 游戏结束判定输家
		bot.sendSticker(chatId.toString(), stickerId = Stickers.killCat.fileId)
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_OVER"].format(currentPlayer.getDisplayName(), currentPlayer.toMentionText())
			enableMarkdown(true)
		}
		// 禁言套餐
		/*RestrictChatMember().apply {
			chatId = this@DeliverBombGame.chatId.toString()
			userId = bombOwner.id
			canSendMessages = false
			untilDate = (System.currentTimeMillis() / 1000L).toInt() + 60
		}.let(bot::execute)*/
		stop()
	}

	override fun onGameInterrupted() {
		timer.cancel()
		bot.replyMessage(collectMessage) {
			text = resources["GAME_INTERRUPTED"]
		}
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
			// 接受游戏停止命令
			"/bomb_game_stop" -> {
				bot.stopEvent<DeliverBombGame>(chatId)
				true
			}
			"/bomb_game_stop@${BotKeystore.botKey.username}" -> {
				bot.stopEvent<DeliverBombGame>(chatId)
				true
			}
			else -> false
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

	override suspend fun onStickerReceived(message: Message): Boolean {
		if (message.sticker.fileId == Stickers.catWithClock.fileId
				&& isPlaying()
				&& findParticipant(message.from) != null) {
			// 接收到参与者的表情
			if (message.from.id != currentPlayer.id) {
				bot.replyMessage(message) {
					text = resources["BOMB_IS_NOT_IN_YOUR_HAND"]
				}
				currentPlayerIndex = indexOfParticipants(message.from)
				stopGame()
			} else {
				val nextOwner: User = if (message.isReply) {
					val replyUser = findParticipant(message.replyToMessage.from)
					if (replyUser == null) {
						// 回复的用户不是参与者
						bot.replyMessage(message) {
							text = resources["REPLY_NON_PARTICIPANT"].format(
									message.replyToMessage.from.getDisplayName())
						}
						return true
					}
					replyUser
				} else {
					participants[(currentPlayerIndex + random.nextInt(1..participants.size)) % participants.size]
				}

				if (random.nextInt(10) < 2) {
					// 运气不佳
					bot.sendMessage(chatId.toString()) {
						text = resources["BOMB_DELIVERED_FAILED"].format(currentPlayer.toMentionText())
						enableMarkdown(true)
					}
					bot.sendSticker(chatId.toString(), stickerId = Stickers.catWithClock.fileId) {
						replyToMessageId = message.messageId
					}
				} else {
					// 传到下一个参与者
					bot.sendMessage(chatId.toString()) {
						text = resources["BOMB_DELIVERED_TO"].format(
								currentPlayer.getDisplayName(),
								nextOwner.getDisplayName(),
								nextOwner.toMentionText()
						)
						enableMarkdown(true)
					}
					currentPlayerIndex = indexOfParticipants(nextOwner)
				}
			}
			return true
		} else {
			return false
		}
	}

	private inner class TickTock : TimerTask() {

		override fun run() {
			if (!isPlaying()) return
			currentTime++
			if (currentTime % 10 == 0) {
				bot.sendMessage(chatId.toString()) {
					text = resources["GAME_TIME_LEFT"].format(MAX_GAME_TIME - currentTime)
				}
			}
			if (currentTime >= MAX_GAME_TIME) {
				stopGame()
			}
		}

	}

	companion object {

		const val MAX_GAME_TIME = 30

		private val resources by resourceBundle("deliver_bomb_game")

	}

}
