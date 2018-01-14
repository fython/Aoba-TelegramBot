package moe.feng.aoba.bot.common

import org.telegram.telegrambots.api.methods.ActionType
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.api.methods.send.SendChatAction
import org.telegram.telegrambots.api.methods.send.SendMessage
import org.telegram.telegrambots.api.methods.send.SendSticker
import org.telegram.telegrambots.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.api.objects.Message
import org.telegram.telegrambots.api.objects.User
import org.telegram.telegrambots.bots.AbsSender
import java.io.Serializable

fun AbsSender.sendMessage(chatId: String, block: SendMessage.() -> Unit): Message? {
	return this.execute(SendMessage().apply { this.chatId = chatId }.apply(block))
}

fun AbsSender.replyMessage(replyTo: Message?, block: SendMessage.() -> Unit): Message? {
	return this.execute(SendMessage().apply { chatId = replyTo?.chatId.toString() }.apply(block).apply {
		replyToMessageId = replyTo?.messageId
	})
}

fun AbsSender.replyMessage(replyToId: Int, block: SendMessage.() -> Unit): Message? {
	return this.execute(SendMessage().apply(block).apply {
		replyToMessageId = replyToId
	})
}

fun AbsSender.editMessageText(originMessage: Message, block: EditMessageText.() -> Unit): Serializable? {
	return this.execute(EditMessageText().apply {
		chatId = originMessage.chatId.toString()
		messageId = originMessage.messageId
		text = originMessage.text
	}.apply(block))
}

fun AbsSender.sendSticker(chatId: String, block: SendSticker.() -> Unit): Message? {
	return this.sendSticker(SendSticker().apply { this.chatId = chatId }.apply(block))
}

fun AbsSender.sendChatAction(chatId: String, action: ActionType): Boolean {
	return this.execute(SendChatAction().apply { this.chatId = chatId; this.action = action })
}

fun AbsSender.hasAdminAccess(user: User, chat: Long): Boolean =
		this.execute(GetChatAdministrators().apply { chatId = chat.toString() }).find { it.user.id == user.id } != null
