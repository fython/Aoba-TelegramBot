package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.AobaBot.Companion.resources
import moe.feng.aoba.bot.common.getDisplayName
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.bot.common.sendMessage
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.support.get

fun AobaBot.registerReplaceFunctions() {
	listenCommand("/replace") { args, message ->
		if (message.isReply && message.replyToMessage.hasText()) {
			if (args.size == 2) {
				StatisticsDao.replaceCommand++
				sendMessage(message.chatId.toString()) {
					text = resources["REPLACE_REPLY_RESULT"].format(
							message.from.getDisplayName(),
							if (message.from.id != message.replyToMessage.from.id) {
								message.replyToMessage.from.getDisplayName()
							} else {
								"Ta"
							},
							message.replyToMessage.text.replace(args[0], "<b>" + args[1] + "</b>")
					)
					enableHtml(true)
				}
			} else {
				replyMessage(message) {
					text = resources["REPLACE_REPLY_WRONG_ARGS"]
				}
			}
		} else {
			replyMessage(message) {
				text = if (args.isEmpty()) {
					resources["REPLACE_NEED_REPLY_TO_MESSAGE_NO_ARGS"]
				} else {
					resources["REPLACE_NEED_REPLY_TO_MESSAGE_HAVE_ARGS"]
				}
			}
		}
		true
	}
}