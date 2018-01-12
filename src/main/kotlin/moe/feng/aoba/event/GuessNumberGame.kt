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

class GuessNumberGame(chatId: Long, bot: BaseTelegramBot) : BaseGame(chatId, bot) {

	private var currentChooserIndex = 0
	private val currentChooser: User get() = participants[currentChooserIndex]

	// 召集信息
	private var collectMessage: Message? = null
	private val collectMarkupInline = InlineKeyboardMarkup()
	private val collectKeyButton = InlineKeyboardButton().apply {
		callbackData = "join_guess_number"
		collectMarkupInline.keyboard = mutableListOf(mutableListOf(this))
	}

	// 随机生成器
	private lateinit var random: Random

	private var min = 0
	private var max = 0
	private var correct = 0

	override fun onStart() {
		random = Random(System.currentTimeMillis())
		// 发送召集信息
		bot.sendSticker(chatId.toString()) {
			sticker = Stickers.konataShoot.fileId
		}
		collectMessage = bot.sendMessage(chatId.toString()) {
			text = resources["GAME_PREPARE"].format(currentChooser.getDisplayName(), makeParticipantsIdList())
			collectKeyButton.text = resources["GAME_JOIN"].format(participants.size)
			replyMarkup = collectMarkupInline
		}
	}

	override fun onGameStart() {
		min = 0
		max = 100 + (participants.size / 2) * 50
		correct = random.nextInt(1 until max)
		// 发送开始通知
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_START"].format(participants.size, "@${currentChooser.userName}")
		}
		printCurrentTurn()
	}

	override fun onGameInterrupted() {
		bot.editMessageText(collectMessage!!) {
			text = resources["GAME_PREPARE"].format(currentChooser.getDisplayName(), makeParticipantsIdList())
			replyMarkup = InlineKeyboardMarkup()
		}
		bot.replyMessage(collectMessage) {
			text = resources["GAME_INTERRUPTED"]
		}
	}

	override fun onGameOver() {
		// 游戏结束判定输家
		bot.sendSticker(chatId.toString()) {
			sticker = Stickers.killCat.fileId
		}
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_OVER"].format(currentChooser.getDisplayName(), currentChooser.userName)
		}
		// 禁言套餐
		RestrictChatMember().apply {
			chatId = this@GuessNumberGame.chatId.toString()
			userId = currentChooser.id
			canSendMessages = false
			untilDate = (System.currentTimeMillis() / 1000L).toInt() + 60
		}.let(bot::execute)
		stop()
	}

	override fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean = when (command) {
		// 接收游戏开始命令
		"/guess_number_game_start" -> {
			onStartCommand(message)
			true
		}
		"/guess_number_game_start@${BotKeystore.botKey.username}" -> {
			onStartCommand(message)
			true
		}
		// 接受游戏停止命令
		"/guess_number_game_stop" -> {
			bot.stopEvent<GuessNumberGame>(chatId)
			true
		}
		"/guess_number_game_stop@${BotKeystore.botKey.username}" -> {
			bot.stopEvent<GuessNumberGame>(chatId)
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
					text = resources["GAME_PREPARE"].format(currentChooser.getDisplayName(), makeParticipantsIdList())
					replyMarkup = InlineKeyboardMarkup()
				}
				startGame()
			}
		}
	}

	private fun printCurrentTurn() {
		bot.sendMessage(chatId.toString()) {
			text = resources["GUESS_TURN_TEXT"].format("@" + currentChooser.userName, min, max)
		}
	}

	override fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean {
		return if (!isPlaying()
				&& callbackQuery.message.messageId == collectMessage?.messageId
				&& callbackQuery.message.chatId == chatId
				&& callbackQuery.data == "join_guess_number") {
			// 响应新玩家加入
			if (findParticipant(callbackQuery.from) == null) {
				participants += callbackQuery.from
			}
			try {
				// 更新召集消息
				bot.editMessageText(collectMessage!!) {
					text = resources["GAME_PREPARE"].format(currentChooser.getDisplayName(), makeParticipantsIdList())
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

	override fun onTextReceived(message: Message): Boolean {
		if (isPlaying() && currentChooser.id == message.from.id) {
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
						currentChooserIndex += 1
						currentChooserIndex %= participants.size
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

	}

}