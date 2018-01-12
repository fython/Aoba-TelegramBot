package moe.feng.aoba.event

import moe.feng.aoba.bot.common.BaseTelegramBot
import moe.feng.aoba.bot.common.TelegramMessageHandler
import org.telegram.telegrambots.api.objects.CallbackQuery
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.User

open class BaseEvent(val chatId: Long, protected val bot: BaseTelegramBot) : TelegramMessageHandler {

	val participants: MutableList<User> = mutableListOf()

	val createTime: Int = (System.currentTimeMillis() / 1000).toInt()

	var isAlive: Boolean = true

	open fun onStart() {}

	open fun onStop() {}

	fun stop() {
		if (isAlive) {
			isAlive = false
			onStop()
		}
	}

	override fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return false
	}

	override fun onTextReceived(message: Message): Boolean {
		return false
	}

	override fun onStickerReceived(message: Message): Boolean {
		return false
	}

	open fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean {
		return false
	}

	fun findParticipant(userId: Int): User? = participants.find { it.id == userId }

	fun findParticipant(user: User): User? = findParticipant(user.id)

	fun indexOfParticipants(user: User): Int = participants.indexOfFirst { it.id == user.id }

	protected fun makeParticipantsIdList(): String = participants.map { "@${it.userName}" }.reduce { acc, s -> "$acc, $s" }

}