package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.support.get

fun AobaBot.registerStatisticsFunctions() {
	listenCommand("/bot_statistics") { _, message ->
		replyMessage(message) {
			text = AobaBot.resources["STATISTICS_FORMAT"].format(
					"@" + BotKeystore.botKey.username,
					StatisticsDao.replaceCommand,
					StatisticsDao.spaceCommand,
					StatisticsDao.chooseCommand,
					StatisticsDao.joinedGroups.size,
					StatisticsDao.bombGame,
					StatisticsDao.guessNumberGame
			)
		}
		true
	}
}