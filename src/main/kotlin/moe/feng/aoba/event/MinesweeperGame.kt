package moe.feng.aoba.event

import moe.feng.aoba.bot.common.*
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.model.MineMap
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.res.Stickers
import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class MinesweeperGame(chatId: Long, bot: BaseTelegramBot) : BaseGame(chatId, bot) {

	// 召集信息
	private var collectMessage: Message? = null
	private val collectMarkupInline = InlineKeyboardMarkup()
	private val collectKeyButton = InlineKeyboardButton().apply {
		callbackData = "join_minesweeper"
		collectMarkupInline.keyboard = mutableListOf(mutableListOf(this))
	}

	private var width = 8
	private var height = 8
	private var mineCount = 9
	private var mapSeed = 0L
	private var openBlockCount = 0
	private var mode = 0

	private lateinit var map: MineMap
	private lateinit var mapState: Array<Array<MapState>>
	private var mapKeyboard: InlineKeyboardMarkup? = null

	private var mapMessage: Message? = null

	override fun onStart() {
		// 发送召集信息
		bot.sendSticker(chatId.toString(), stickerId = Stickers.catWithClock.fileId)
		collectMessage = bot.sendMessage(chatId.toString()) {
			text = resources["GAME_PREPARE"].format(currentPlayer.getDisplayName(), makeParticipantsIdList())
			collectKeyButton.text = baseResources["GAME_JOIN"].format(participants.size)
			replyMarkup = collectMarkupInline
		}
	}

	override fun onGameStart() {
		StatisticsDao.minesweeperGame++
		// 发送开始通知
		bot.sendMessage(chatId.toString()) {
			text = resources["GAME_START"].format(participants.size, when (mode) {
				0 -> resources["MODE_FREE"]
				else -> null
			})
		}

		if (mapSeed <= 0L) {
			mapSeed = System.currentTimeMillis()
		}
		map = MineMap(mapSeed, width, height, mineCount)
		mapState = Array(height) { Array(width) { MapState.NORMAL } }
		openBlockCount = 0
		mapKeyboard = null

		sendMapMessage(false)
	}

	override fun onGameOver() {
		stop()
	}

	override fun onGameInterrupted() {
		bot.replyMessage(collectMessage) {
			text = resources["GAME_INTERRUPTED"]
		}
	}

	override fun onStop() {
		super.onStop()
		try {
			bot.editMessageText(collectMessage!!) {
				text = resources["GAME_PREPARE"].format(currentPlayer.getDisplayName(), makeParticipantsIdList())
				replyMarkup = InlineKeyboardMarkup()
			}
		} catch (e : TelegramApiException) {

		}
		if (mapMessage != null) {
			try {
				bot.editMessageText(mapMessage!!) {
					text = mapMessage!!.text
					replyMarkup = InlineKeyboardMarkup()
				}
			} catch (e : TelegramApiException) {

			}
		}
	}

	private fun sendMapMessage(useEdit: Boolean = false) {
		/*if (mapMessage != null) {
			DeleteMessage().apply {
				chatId = this@MinesweeperGame.chatId.toString()
				messageId = mapMessage?.messageId
			}.let(bot::execute)
		}*/
		if (useEdit && mapMessage != null) {
			bot.editMessageText(mapMessage!!) {
				replyMarkup = updateKeyboard()
			}
		} else {
			mapMessage = bot.sendMessage(chatId.toString()) {
				text = when (mode) {
					0 -> resources["GAME_MESSAGE_FREE_MODE"].format(
							resources["GAME_MESSAGE_MAP_INFO"].format(
									map.width,
									map.height,
									map.seed.toString(),
									map.mineCount
							)
					)
					else -> null
				}
				replyMarkup = updateKeyboard()
			}
		}
	}

	private fun updateKeyboard(): InlineKeyboardMarkup {
		if (mapKeyboard == null) {
			mapKeyboard = InlineKeyboardMarkup().apply {
				keyboard = MutableList(height) { x ->
					MutableList(width) { y ->
						InlineKeyboardButton().apply {
							text = BLOCK_TEXT
							callbackData = "minesweeper,$x,$y"
						}
					}
				}
			}
		}
		for ((x, line) in mapKeyboard!!.keyboard!!.withIndex()) {
			for ((y, item) in line.withIndex()) {
				item.text = when (mapState[x][y]) {
					MapState.NORMAL -> BLOCK_TEXT
					MapState.OPEN -> NUMBER_TEXT[map.map[x][y]]
					MapState.EXPLODED -> NUMBER_TEXT[9]
				}
			}
		}
		return mapKeyboard!!
	}

	override suspend fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return when (command) {
			"/minesweeper_game_start" -> {
				onStartCommand(args, message)
				true
			}
			"/minesweeper_game_start@${BotKeystore.botKey.username}" -> {
				onStartCommand(args, message)
				true
			}
			"/minesweeper_game_stop" -> {
				bot.stopEvent<MinesweeperGame>(chatId)
				true
			}
			"/minesweeper_game_stop@${BotKeystore.botKey.username}" -> {
				bot.stopEvent<MinesweeperGame>(chatId)
				true
			}
			else -> super.onCommandReceived(command, args, message)
		}
	}

	private fun onStartCommand(args: List<String>, message: Message) {
		if (isPlaying()) {
			bot.replyMessage(message) {
				text = resources["GAME_ALREADY_START"]
			}
		} else {
			if (args.isNotEmpty()) {
				try {
					when (args.size) {
						 3 -> {
							val (h, w, count) = args
							height = h.toInt()
							width = w.toInt()
							mineCount = count.toInt()
						}
						4 -> {
							val (h, w, count, seed) = args
							height = h.toInt()
							width = w.toInt()
							mineCount = count.toInt()
							mapSeed = seed.toLong()
						}
					}
					if (width !in 0..10 || height !in 0..10 ||
							(mineCount < 1) || (mineCount >= height * width) || mapSeed < 0) {
						throw IllegalArgumentException()
					}
				} catch (e : Exception) {
					sendStartFailedMessage(message)
					return
				}
			} else {
				width = 8
				height = 8
				mineCount = 9
				mapSeed = 0L
			}
			bot.editMessageText(collectMessage!!) {
				text = resources["GAME_PREPARE"].format(currentPlayer.getDisplayName(), makeParticipantsIdList())
				replyMarkup = InlineKeyboardMarkup()
			}
			startGame()
		}
	}

	private fun sendStartFailedMessage(message: Message) {
		bot.replyMessage(message) {
			text = resources["GAME_START_FAILED"]
		}
	}

	override fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean {
		if (!isPlaying()
				&& callbackQuery.message.messageId == collectMessage?.messageId
				&& callbackQuery.message.chatId == chatId
				&& callbackQuery.data == "join_minesweeper") {
			// 响应新玩家加入
			if (findParticipant(callbackQuery.from) == null) {
				participants += callbackQuery.from
			}
			try {
				// 更新召集消息
				bot.editMessageText(collectMessage!!) {
					text = resources["GAME_PREPARE"].format(currentPlayer.getDisplayName(), makeParticipantsIdList())
					collectKeyButton.text = baseResources["GAME_JOIN"].format(participants.size)
					replyMarkup = collectMarkupInline
				}
			} catch (e: TelegramApiException) {
				e.printStackTrace()
			}
			return true
		} else if (isPlaying()
				&& callbackQuery.message.chatId == chatId
				&& participants.find { it.id == callbackQuery.from.id } != null) {
			if (callbackQuery.data.startsWith("minesweeper")) {
				val (x, y) = callbackQuery.data.split(",").drop(1).map(String::toInt)
				if (mapState[x][y] == MapState.NORMAL) {
					if (map.map[x][y] == 9) {
						mapState[x][y] = MapState.EXPLODED
						bot.sendMessage(chatId.toString()) {
							text = resources["GAME_OVER_ONE_BOOM"].format("@" + callbackQuery.from.userName)
						}
						stopGame()
					} else {
						openBlock(x, y)
						if (width * height - openBlockCount == mineCount) {
							bot.sendMessage(chatId.toString()) {
								text = resources["GAME_OVER_CLEAR_ALL_BOMB"]
							}
							stopGame()
						}
					}
					sendMapMessage(true)
					return true
				}
			}
		}
		return false
	}

	private fun openBlock(x: Int, y: Int) {
		if (x in 0 until height && y in 0 until width && mapState[x][y] == MapState.NORMAL) {
			openBlockCount++
			mapState[x][y] = MapState.OPEN
			if (map.map[x][y] == 0) {
				for ((xOffset, yOffset) in (-1..1).flatMap { xOffset -> (-1..1).map { yOffset -> xOffset to yOffset } }) {
					openBlock(x + xOffset, y + yOffset)
				}
			}
		}
	}

	enum class MapState {
		NORMAL, OPEN, EXPLODED
	}

	companion object {

		val NUMBER_TEXT = arrayOf("　", "１", "２", "３", "４", "５", "６", "７", "８", "＊")

		const val BLOCK_TEXT = "█"

		private val resources by resourceBundle("minesweeper_game")

	}

}