package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.bot.common.sendChatAction
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.support.get
import moe.feng.aoba.support.randomOne
import org.telegram.telegrambots.api.methods.ActionType

fun AobaBot.registerHelpMeChooseFunction() {
	listenCommand("/choose") { args, message ->
		if (args.isEmpty()) {
			replyMessage(message) {
				text = AobaBot.resources["HELP_ME_CHOOSE_HELP"]
			}
		} else {
			StatisticsDao.chooseCommand++
			sendChatAction(message.chatId.toString(), ActionType.TYPING)
			Thread.sleep(3000)
			replyMessage(message) {
				text = AobaBot.resources["HELP_ME_CHOOSE_RESULT"].format(args.randomOne())
			}
		}
		true
	}
}