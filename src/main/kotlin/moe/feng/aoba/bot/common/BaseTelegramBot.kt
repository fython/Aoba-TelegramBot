package moe.feng.aoba.bot.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import moe.feng.aoba.event.BaseEvent
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.objects.*
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker

abstract class BaseTelegramBot(
		private val botKey: BotKey,
		private val botOptions: DefaultBotOptions = DefaultBotOptions()
) : TelegramLongPollingBot(botOptions), TelegramMessageHandler, CallbackQueryHandler, CoroutineScope by MainScope() {

	private val commandsCallbacks: MutableMap<String, suspend (List<String>, Message) -> Boolean> = mutableMapOf()
	private val textKeywordsCallbacks: MutableList<Pair<Array<out String>, suspend (Message) -> Boolean>> = mutableListOf()
	private val stickersCallbacks: MutableList<Pair<Sticker, suspend (Message) -> Boolean>> = mutableListOf()
	private val callbackQueryCallbacks: MutableList<Pair<String, suspend (CallbackQuery) -> Boolean>> = mutableListOf()

	private val createTime: Int = (System.currentTimeMillis() / 1000).toInt()

	private val events: MutableMap<String, BaseEvent> = mutableMapOf()

	fun listenCommand(command: String, callback: suspend (args: List<String>, message: Message) -> Boolean) {
		commandsCallbacks += command to callback
		commandsCallbacks += "$command@$botUsername" to callback
 	}

	fun listenKeywords(vararg keywords: String, callback: suspend (message: Message) -> Boolean) {
		textKeywordsCallbacks += keywords to callback
	}

	fun listenSticker(sticker: Sticker, callback: suspend (message: Message) -> Boolean) {
		stickersCallbacks += sticker to callback
	}

	override fun listenCallbackQuery(callbackData: String, callback: suspend (callbackQuery: CallbackQuery) -> Boolean) {
		callbackQueryCallbacks += callbackData to callback
	}

	fun <T : BaseEvent> findEvent(eventClazz: Class<T>, chatId: Long): T? {
		return events["${eventClazz.canonicalName}#$chatId"] as? T
	}

	inline fun <reified T : BaseEvent> findEvent(chatId: Long): T? {
		return findEvent(T::class.java, chatId)
	}

	fun startEvent(chatId: Long, event: BaseEvent) {
		try {
			findEvent(event.javaClass, chatId)?.stop()
		} catch (e : Exception) {
			e.printStackTrace()
		}
		events["${event.javaClass.canonicalName}#$chatId"] = event
		event.onStart()
	}

	fun <T : BaseEvent> stopEvent(chatId: Long, eventClazz: Class<T>) {
		try {
			findEvent(eventClazz, chatId)?.stop()
		} catch (e : Exception) {
			e.printStackTrace()
		}
		events.remove("${eventClazz.canonicalName}#$chatId")
	}

	inline fun <reified T : BaseEvent> stopEvent(chatId: Long) {
		stopEvent(chatId, T::class.java)
	}

	override fun getBotToken() = botKey.token
	override fun getBotUsername() = botKey.username

	open fun isAllowedBeUsed(chat: Chat): Boolean = true

	open fun isAllowedReceiveOldMessage(): Boolean = true

	fun isAdmin(chat: Chat): Boolean {
		return hasAdminAccess(me, chat.id)
	}

	fun isAdmin(chatId: Long): Boolean {
		return hasAdminAccess(me, chatId)
	}

	final override fun onUpdateReceived(update: Update?) {
		launch(Dispatchers.IO) {
			if (update?.hasMessage() == true) {
				val msg = update.message

				if (!isAllowedBeUsed(msg.chat)) return@launch
				if (isAllowedReceiveOldMessage() || update.message.date < createTime) return@launch

				for ((_, event) in events.filterKeys { key -> "#${msg.chatId}" in key }.toList()) {
					if (event.isAlive) {
						if (event.handleMessage(msg)) {
							return@launch
						}
					} else {
						stopEvent(msg.chatId, event.javaClass)
					}
				}
				if (handleMessage(msg)) {
					return@launch
				}
			} else if (update?.hasCallbackQuery() == true) {
				var proceed = false
				for ((_, event) in events) {
					if (event.onCallbackQuery(update.callbackQuery)) {
						proceed = true
						continue
					}
				}
				if (!proceed) {
					onCallbackQuery(update.callbackQuery)
				}
			}
		}
	}

	override suspend fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return commandsCallbacks[command]?.invoke(args, message) ?: false
	}

	override suspend fun onTextReceived(message: Message): Boolean {
		return textKeywordsCallbacks.any { (keywords, callback) ->
			(keywords.find { message.text.contains(it, ignoreCase = false) } != null) && callback(message)
		}
	}

	override suspend fun onStickerReceived(message: Message): Boolean {
		return stickersCallbacks.find { (sticker, _) ->
			sticker.fileId == message.sticker.fileId
		}?.second?.invoke(message) ?: false
	}

	override suspend fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean {
		return callbackQueryCallbacks.find { (data, _) ->
			callbackQuery.data == data
		}?.second?.invoke(callbackQuery) ?: false
	}

	override suspend fun onNewChatMembers(message: Message, members: List<User>): Boolean {
		return false
	}

	override suspend fun onLeftChatMembers(message: Message, member: User): Boolean {
		return false
	}

	data class BotKey(val token: String, val username: String)

}