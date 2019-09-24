package moe.feng.aoba.bot.common

import org.apache.tools.ant.types.Commandline
import org.telegram.telegrambots.meta.api.objects.Message

interface TelegramMessageHandler {

	fun onCommandReceived(command: String, args: List<String>, message: Message): Boolean

	fun onTextReceived(message: Message): Boolean

	fun onStickerReceived(message: Message): Boolean

	fun handleMessage(msg: Message): Boolean {
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
		return false
	}

}