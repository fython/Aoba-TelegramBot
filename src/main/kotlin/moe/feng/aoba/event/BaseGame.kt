package moe.feng.aoba.event

import moe.feng.aoba.bot.common.BaseTelegramBot
import moe.feng.aoba.support.resourceBundle
import org.telegram.telegrambots.meta.api.objects.User
import java.util.*

open class BaseGame(chatId: Long, bot: BaseTelegramBot) : BaseEvent(chatId, bot) {

	protected var currentPlayerIndex = 0
	protected val currentPlayer: User get() = participants[currentPlayerIndex]

	private var isPlaying = false
	protected val random = Random(System.currentTimeMillis())

	open fun onGameStart() {}

	open fun onGameOver() {
		stop()
	}

	open fun onGameInterrupted() {

	}

	override fun onStop() {
		if (isPlaying) {
			onGameInterrupted()
		}
		super.onStop()
	}

	fun startGame() {
		if (!isPlaying) {
			isPlaying = true
			onGameStart()
		}
	}

	fun stopGame() {
		isPlaying = false
		onGameOver()
	}

	fun isPlaying(): Boolean = isPlaying

	companion object {

		@JvmStatic val baseResources by resourceBundle("base_game")

	}

}