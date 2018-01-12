package moe.feng.aoba.bot.common

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import moe.feng.aoba.event.BaseEvent
import org.telegram.telegrambots.api.objects.Chat
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.Update
import org.telegram.telegrambots.api.objects.stickers.Sticker
import org.telegram.telegrambots.bots.TelegramLongPollingBot

abstract class BaseTelegramBot(private val botKey: BotKey) : TelegramLongPollingBot(), TelegramMessageHandler {

	private val commandsCallbacks: MutableMap<String, (List<String>, Message) -> Boolean> = mutableMapOf()
	private val textKeywordsCallbacks: MutableList<Pair<Array<out String>, (Message) -> Boolean>> = mutableListOf()
	private val stickersCallbacks: MutableList<Pair<Sticker, (Message) -> Boolean>> = mutableListOf()

	private val createTime: Int = (System.currentTimeMillis() / 1000).toInt()

	private val events: MutableMap<String, BaseEvent> = mutableMapOf()

	fun listenCommand(command: String, callback: (args: List<String>, message: Message) -> Boolean) {
		commandsCallbacks += command to callback
		commandsCallbacks += (command + "@" + botUsername) to callback
 	}

	fun listenKeywords(vararg keywords: String, callback: (message: Message) -> Boolean) {
		textKeywordsCallbacks += keywords to callback
	}

	fun listenSticker(sticker: Sticker, callback: (message: Message) -> Boolean) {
		stickersCallbacks += sticker to callback
	}

	fun <T : BaseEvent> findEvent(eventClazz: Class<T>, chatId: Long): T? {
		return events["${eventClazz.canonicalName}#$chatId"] as? T
	}

	inline fun <reified T : BaseEvent> findEvent(chatId: Long): T? {
		return findEvent(T::class.java, chatId)
	}

	fun startEvent(chatId: Long, event: BaseEvent) {
		findEvent(event.javaClass, chatId)?.stop()
		events["${event.javaClass.canonicalName}#$chatId"] = event
		event.onStart()
	}

	fun <T : BaseEvent> stopEvent(chatId: Long, eventClazz: Class<T>) {
		findEvent(eventClazz, chatId)?.stop()
		events.remove("${eventClazz.canonicalName}#$chatId")
	}

	inline fun <reified T : BaseEvent> stopEvent(chatId: Long) {
		stopEvent(chatId, T::class.java)
	}

	override fun getBotToken() = botKey.token
	override fun getBotUsername() = botKey.username

	open fun isAllowedBeUsed(chat: Chat): Boolean = true

	open fun isAllowedReceiveOldMessage(): Boolean = true

	override final fun onUpdateReceived(update: Update?) {
		launch(CommonPool) {
			if (update?.hasMessage() == true) {
				val msg = update.message

				if (!isAllowedBeUsed(msg.chat)) return@launch
				if (isAllowedReceiveOldMessage() || update.message.date < createTime) return@launch

				for ((_, event) in events.filterKeys { key -> key.contains("#${msg.chatId}") }.toList()) {
					if (event.handleMessage(msg)) {
						return@launch
					}
				}
				if (handleMessage(msg)) {
					return@launch
				}
			} else if (update?.hasCallbackQuery() == true) {
				events.forEach { _, event ->
					if (event.onCallbackQuery(update.callbackQuery)) {
						return@forEach
					}
				}
			}
		}
	}

	override fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean {
		return commandsCallbacks[command]?.invoke(args, message) ?: false
	}

	override fun onTextReceived(message: Message): Boolean {
		return textKeywordsCallbacks.find { (keywords, callback) ->
			(keywords.find { message.text.contains(it, ignoreCase = false) } != null) && callback(message)
		} != null
	}

	override fun onStickerReceived(message: Message): Boolean {
		return stickersCallbacks.find { (sticker, _) ->
			sticker.fileId == message.sticker.fileId
		}?.second?.invoke(message) ?: false
	}

	data class BotKey(val token: String, val username: String)

}