package moe.feng.aoba.bot.common

import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

interface CallbackQueryHandler {

    fun listenCallbackQuery(callbackData: String, callback: suspend (callbackQuery: CallbackQuery) -> Boolean)

    fun createInlineKeyboardButton(
            text: String? = null, data: String? = null, url: String? = null,
            callback: (suspend (callbackQuery: CallbackQuery) -> Boolean)? = null
    ): InlineKeyboardButton {
        if (data != null && callback != null) {
            listenCallbackQuery(data, callback)
        }
        return InlineKeyboardButton().apply {
            this.callbackData = data
            this.text = text
            this.url = url
        }
    }

}