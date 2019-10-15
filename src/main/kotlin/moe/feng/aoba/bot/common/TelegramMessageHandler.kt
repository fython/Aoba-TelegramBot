package moe.feng.aoba.bot.common

import org.apache.tools.ant.types.Commandline
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User

interface TelegramMessageHandler {

	suspend fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean

	suspend fun onTextReceived(message: Message): Boolean

	suspend fun onStickerReceived(message: Message): Boolean

	suspend fun onCallbackQuery(callbackQuery: CallbackQuery): Boolean

	suspend fun onNewChatMembers(message: Message, members: List<User>): Boolean

	suspend fun onLeftChatMembers(message: Message, member: User): Boolean

	suspend fun handleMessage(msg: Message): Boolean {
		if (msg.hasText()) {
			if (msg.text.startsWith("/")) {
				val commands = Commandline.translateCommandline(msg.text)
				if (onCommandReceived(commands[0], commands.drop(1), msg)) {
					// The command has been handled.
					return true
				}
			}

			if (onTextReceived(msg)) {
				return true
			}
		}
		if (msg.sticker != null) {
			if (onStickerReceived(msg)) {
				return true
			}
		}
		if (msg.newChatMembers != null) {
			if (onNewChatMembers(msg, msg.newChatMembers)) {
				return true
			}
		}
		if (msg.leftChatMember != null) {
			if (onLeftChatMembers(msg, msg.leftChatMember)) {
				return true
			}
		}
		return false
	}

}