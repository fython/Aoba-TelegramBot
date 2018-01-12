package moe.feng.aoba.event

import moe.feng.aoba.bot.common.BaseTelegramBot

open class BaseGame(chatId: Long, bot: BaseTelegramBot) : BaseEvent(chatId, bot) {

	private var isPlaying = false

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
		isPlaying = true
		onGameStart()
	}

	fun stopGame() {
		isPlaying = false
		onGameOver()
	}

	fun isPlaying(): Boolean = isPlaying

}