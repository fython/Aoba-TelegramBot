package moe.feng.aoba.bot.functions

import kotlinx.coroutines.experimental.async
import moe.feng.aoba.api.RemoteYeelightApi
import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.editMessageText
import moe.feng.aoba.bot.common.replyMessage

fun AobaBot.registerYeelightFunctions() {
	listenCommand("/toggle_light") { _, message ->
		async {
			if (!RemoteYeelightApi.isOnline()) {
				replyMessage(message) {
					text = "台灯不在线ww"
				}
			} else {
				val reply = replyMessage(message) {
					text = "正在执行台灯开关切换操作……"
				}
				val result = RemoteYeelightApi.toggle()
				reply?.let {
					editMessageText(it) {
						text = "台灯开关切换结果：\n`$result`"
						enableMarkdown(true)
					}
				}
			}
		}
		true
	}
}