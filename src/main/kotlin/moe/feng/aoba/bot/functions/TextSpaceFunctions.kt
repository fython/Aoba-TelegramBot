package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.getDisplayName
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.bot.common.sendMessage
import moe.feng.aoba.support.isChinese

fun AobaBot.registerTextSpaceFunctions() {
	listenCommand("/add_space") { _, message ->
		if (message.isReply && message.replyToMessage.hasText()) {
			var newText = message.replyToMessage.text.trim()
			for ((i, c) in newText.withIndex().reversed()) {
				if (c.isChinese() || newText.getOrElse(i - 1, {'_'}).isChinese()) {
					newText = newText.substring(0, i) + " " + newText.substring(i, newText.length)
				}
			}
			sendMessage(message.chatId.toString()) {
				text = message.replyToMessage.from.getDisplayName() + ": " + newText
			}
		} else {
			replyMessage(message) {
				text = "请选择一条文本信息进行回复。"
			}
		}
		true
	}
	listenCommand("/remove_space") { _, message ->
		if (message.isReply && message.replyToMessage.hasText()) {
			var newText = message.replyToMessage.text.trim()
			for ((i, c) in newText.withIndex().reversed()) {
				if (c == ' '
						&& newText.getOrElse(i - 1, {'_'} ).isChinese()
						&& newText.getOrElse(i + 1, {'_'} ).isChinese()) {
					newText = newText.removeRange(i, i + 1)
				}
			}
			sendMessage(message.chatId.toString()) {
				text = message.replyToMessage.from.getDisplayName() + ": " + newText
			}
		} else {
			replyMessage(message) {
				text = "请选择一条文本信息进行回复。"
			}
		}
		true
	}
}