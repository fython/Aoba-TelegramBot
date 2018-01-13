package moe.feng.aoba.event

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.res.Stickers
import moe.feng.aoba.support.get
import moe.feng.aoba.support.nextInt
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.api.methods.groupadministration.RestrictChatMember
import org.telegram.telegrambots.api.objects.CallbackQuery
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.User
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.exceptions.TelegramApiException
import java.util.*

class DeliverBombGame(chatId: Long, bot: BaseTelegramBot) : BaseGame(chatId, bot) {

	// 炸弹拥有者
	private var bombOwnerIndex: Int = 0
	private val bombOwner: User get() = participants[bombOwnerIndex]

	// 召集信息
	private var collectMessage: Message? = null
	private val collectMarkupInline = InlineKeyboardMarkup()
	private val collectKeyButton = InlineKeyboardButton().apply {
		callbackData = "join"
		collectMarkupInline.keyboard = mutableListOf(mutableListOf(this))
	}

	// 随机生成器
	private lateinit var random: Random

	// 时间计数器
	private var currentTime = 0
	private val timer = Timer()

	override fun onStart() {
		random = Random(System.currentTimeMillis())
		// 发送召集信息
		bot.sendSticker(chatId.toString()) {
			sticker = Stickers.catWithClock.fileId
		}
		collectMessage = bot.sendMessage(chatId.toString()) {
			text = resources["GAME_PREPARE"].format(bombOwner.getDisplayName(), makeParticipantsIdList())
			collectKeyButton.text = resources["GAME_JOIN"].format(participants.size)
			replyMarkup = collectMarkupInline
		}
	}

	override fun onGameStart() {
		// 发送开始通知
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_START"].format(participants.size, "@${bombOwner.userName}", MAX_GAME_TIME)
		}
		// 设定定时器
		timer.scheduleAtFixedRate(TickTock(), 0, 1000)
	}

	override fun onGameOver() {
		timer.cancel()
		// 游戏结束判定输家
		bot.sendSticker(chatId.toString()) {
			sticker = Stickers.killCat.fileId
		}
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_OVER"].format(bombOwner.getDisplayName(), bombOwner.userName)
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
		println("onGameInterrupted")
		timer.cancel()
		bot.replyMessage(collectMessage) {
			text = resources["GAME_INTERRUPTED"]
		}
	}

	override fun onStop() {
		super.onStop()
		try {
			bot.editMessageText(collectMessage!!) {
				text = resources["GAME_PREPARE"].format(bombOwner.getDisplayName(), makeParticipantsIdList())
				replyMarkup = InlineKeyboardMarkup()
			}
		} catch (e : TelegramApiException) {

		}
	}

	override fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean = when (command) {
		// 接收游戏开始命令
		"/bomb_game_start" -> {
			onStartCommand(message)
			true
		}
		"/bomb_game_start@${BotKeystore.botKey.username}" -> {
			onStartCommand(message)
			true
		}
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

	private fun onStartCommand(message: Message) {
		if (isPlaying()) {
			bot.replyMessage(message) {
				text = resources["GAME_ALREADY_START"]
			}
		} else {
			if (participants.size <= 1) {
				bot.replyMessage(message) {
					text = resources["GAME_NEED_MORE_PARTICIPANTS"]
				}
			} else {
				bot.editMessageText(collectMessage!!) {
					text = resources["GAME_PREPARE"].format(bombOwner.getDisplayName(), makeParticipantsIdList())
					replyMarkup = InlineKeyboardMarkup()
				}
				startGame()
			}
		}
	}

	override fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean {
		return if (!isPlaying()
				&& callbackQuery.message.messageId == collectMessage?.messageId
				&& callbackQuery.message.chatId == chatId
				&& callbackQuery.data == "join") {
			// 响应新玩家加入
			if (findParticipant(callbackQuery.from) == null) {
				participants += callbackQuery.from
			}
			try {
				// 更新召集消息
				bot.editMessageText(collectMessage!!) {
					text = resources["GAME_PREPARE"].format(bombOwner.getDisplayName(), makeParticipantsIdList())
					collectKeyButton.text = resources["GAME_JOIN"].format(participants.size)
					replyMarkup = collectMarkupInline
				}
			} catch (e: TelegramApiException) {
				e.printStackTrace()
			}
			true
		} else {
			false
		}
	}

	override fun onStickerReceived(message: Message): Boolean {
		if (message.sticker.fileId == Stickers.catWithClock.fileId
				&& isPlaying()
				&& findParticipant(message.from) != null) {
			// 接收到参与者的表情
			if (message.from.id != bombOwner.id) {
				bot.replyMessage(message) {
					text = resources["BOMB_IS_NOT_IN_YOUR_HAND"]
				}
				bombOwnerIndex = indexOfParticipants(message.from)
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
					participants[(bombOwnerIndex + random.nextInt(1..participants.size)) % participants.size]
				}

				if (random.nextInt(10) < 2) {
					// 运气不佳
					bot.sendMessage(chatId.toString()) {
						text = resources["BOMB_DELIVERED_FAILED"].format("@" + bombOwner.userName)
					}
					bot.sendSticker(chatId.toString()) {
						sticker = Stickers.catWithClock.fileId
						replyToMessageId = message.messageId
					}
				} else {
					// 传到下一个参与者
					bot.sendMessage(chatId.toString()) {
						text = resources["BOMB_DELIVERED_TO"].format(
								bombOwner.getDisplayName(),
								nextOwner.getDisplayName(),
								"@${nextOwner.userName}"
						)
					}
					bombOwnerIndex = indexOfParticipants(nextOwner)
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
			println("Current time: $currentTime")
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