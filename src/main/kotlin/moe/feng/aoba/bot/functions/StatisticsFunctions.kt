package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.res.BotKeystore
import moe.feng.aoba.support.get
import java.text.SimpleDateFormat
import java.util.*

fun AobaBot.registerStatisticsFunctions() {
	listenCommand("/bot_statistics") { _, message ->
		replyMessage(message) {
			text = AobaBot.resources["STATISTICS_FORMAT"].format(
					/* Bot name = */ "@" + BotKeystore.botKey.username,
					StatisticsDao.replaceCommand,
					StatisticsDao.spaceCommand,
					StatisticsDao.chooseCommand,
					StatisticsDao.joinedGroups.size,
					StatisticsDao.bombGame,
					StatisticsDao.guessNumberGame,
					/* Last launch time = */ SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(StatisticsDao.lastLaunchTime)),
					(System.currentTimeMillis() - StatisticsDao.lastLaunchTime).let {
						val s = it / 1000L % 60
						val m = it / 1000L / 60 % 60
						val h = it / 1000L / 60 / 60 % 24
						val d = it / 1000L / 60 / 60 / 24
						AobaBot.resources["TIME_FORMAT"].format(d, h, m, s)
					}
			)
		}
		true
	}
}