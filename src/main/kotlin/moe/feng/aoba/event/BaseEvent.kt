package moe.feng.aoba.event

import moe.feng.aoba.bot.common.BaseTelegramBot
import moe.feng.aoba.bot.common.CallbackQueryHandler
import moe.feng.aoba.bot.common.TelegramMessageHandler
import moe.feng.aoba.bot.common.toMentionText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User

open class BaseEvent(val chatId: Long, protected val bot: BaseTelegramBot)
	: TelegramMessageHandler, CallbackQueryHandler {

	private val callbackQueryCallbacks: MutableList<Pair<String, suspend (CallbackQuery) -> Boolean>> = mutableListOf()

	override fun listenCallbackQuery(callbackData: String, callback: suspend (callbackQuery: CallbackQuery) -> Boolean) {
		callbackQueryCallbacks += callbackData to callback
	}

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

	override suspend fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return false
	}

	override suspend fun onTextReceived(message: Message): Boolean {
		return false
	}

	override suspend fun onStickerReceived(message: Message): Boolean {
		return false
	}

	override suspend fun onNewChatMembers(message: Message, members: List<User>): Boolean {
		return false
	}

	override suspend fun onLeftChatMembers(message: Message, member: User): Boolean {
		return false
	}

	override suspend fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean {
		return callbackQueryCallbacks.find { (data, _) ->
			callbackQuery.data == data
		}?.second?.invoke(callbackQuery) ?: false
	}

	fun findParticipant(userId: Int): User? = participants.find { it.id == userId }

	fun findParticipant(user: User): User? = findParticipant(user.id)

	fun indexOfParticipants(user: User): Int = participants.indexOfFirst { it.id == user.id }

	protected fun makeParticipantsIdList(): String = participants.map(User::toMentionText).reduce { acc, s -> "$acc, $s" }

}