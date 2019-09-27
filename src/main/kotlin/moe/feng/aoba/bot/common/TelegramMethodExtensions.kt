package moe.feng.aoba.bot.common

import org.telegram.telegrambots.meta.api.methods.ActionType
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.bots.AbsSender
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

fun AbsSender.sendSticker(chatId: String, stickerId: String? = null, stickerFile: InputFile? = null,
						  block: SendSticker.() -> Unit = {}): Message? {
	return this.execute(SendSticker().apply { this.chatId = chatId }.apply(block).apply {
		if (stickerId != null) {
			setSticker(stickerId)
		} else if (stickerFile != null) {
			sticker = stickerFile
		}
		if (sticker == null) {
			throw IllegalArgumentException("Cannot send empty sticker")
		}
	})
}

fun AbsSender.sendChatAction(chatId: String, action: ActionType): Boolean {
	return this.execute(SendChatAction().apply { this.chatId = chatId; this.action = action })
}

fun AbsSender.hasAdminAccess(user: User, chat: Long): Boolean {
	return this.execute(GetChatAdministrators().apply { chatId = chat.toString() })
			.find { it.user.id == user.id } != null
}

fun AbsSender.answerCallbackQuery(id: String, block: AnswerCallbackQuery.() -> Unit): Boolean {
	return this.execute(AnswerCallbackQuery().apply {
		this.callbackQueryId = id
		block()
	})
}

fun AbsSender.answerCallbackQuery(callbackQuery: CallbackQuery, block: AnswerCallbackQuery.() -> Unit): Boolean {
	return answerCallbackQuery(callbackQuery.id, block)
}