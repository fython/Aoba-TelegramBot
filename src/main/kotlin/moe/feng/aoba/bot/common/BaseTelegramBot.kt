package moe.feng.aoba.bot.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import moe.feng.aoba.event.BaseEvent
import org.telegram.telegrambots.bots.DefaultBotOptions
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker

abstract class BaseTelegramBot(
		private val botKey: BotKey,
		private val botOptions: DefaultBotOptions = DefaultBotOptions()
) : TelegramLongPollingBot(botOptions), TelegramMessageHandler, CoroutineScope by MainScope() {

	private val commandsCallbacks: MutableMap<String, suspend (List<String>, Message) -> Boolean> = mutableMapOf()
	private val textKeywordsCallbacks: MutableList<Pair<Array<out String>, suspend (Message) -> Boolean>> = mutableListOf()
	private val stickersCallbacks: MutableList<Pair<Sticker, suspend (Message) -> Boolean>> = mutableListOf()

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

	final override fun onUpdateReceived(update: Update?) {
		launch(Dispatchers.IO) {
			if (update?.hasMessage() == true) {
				val msg = update.message

				if (!isAllowedBeUsed(msg.chat)) return@launch
				if (isAllowedReceiveOldMessage() || update.message.date < createTime) return@launch

				for ((_, event) in events.filterKeys { key -> key.contains("#${msg.chatId}") }.toList()) {
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
				for ((_, event) in events) {
					if (event.onCallbackQuery(update.callbackQuery)) {
						continue
					}
				}
			}
		}
	}

	override suspend fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return commandsCallbacks[command]?.invoke(args, message) ?: false
	}

	override suspend fun onTextReceived(message: Message): Boolean {
		return textKeywordsCallbacks.find { (keywords, callback) ->
			(keywords.find { message.text.contains(it, ignoreCase = false) } != null) && callback(message)
		} != null
	}

	override suspend fun onStickerReceived(message: Message): Boolean {
		return stickersCallbacks.find { (sticker, _) ->
			sticker.fileId == message.sticker.fileId
		}?.second?.invoke(message) ?: false
	}

	data class BotKey(val token: String, val username: String)

}