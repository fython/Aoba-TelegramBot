package moe.feng.aoba.bot.functions

import moe.feng.aoba.bot.AobaBot
import moe.feng.aoba.bot.common.replyMessage
import moe.feng.aoba.bot.common.sendMessage

fun AobaBot.registerHelpFunctions() {
	listenCommand("/start") { _, message ->
		if (message.chat.isUserChat) {
			sendMessage(message.chatId.toString()) {
				text = """
					欢迎使用烧饼的 Bot~
					本机器人由 @fython 进行开发维护，如有建议可向作者反馈。使用说明请输入 /help 命令。
					代码在 GPL v3 协议下公开：https://github.com/fython/Aoba-TelegramBot
					""".trimIndent()
			}
		}
		true
	}
	listenCommand("/help") { _, message ->
		replyMessage(message) {
			text = if (message.chat.isUserChat) {
				"""
					烧饼 Bot 使用说明~

					/replace - 替换一句话中的关键词为另一个词
					/add_space - 每个汉字都使用空格隔开
					/remove_space - 去掉汉字间的空格
					/allow_game - 查询/设置当前群组是否允许发起游戏（仅管理员可设置）
					/bot_statistics - 查看机器人全局统计数据

					已支持的游戏（规则请调用命令后查看）：
					/bomb_game - 开始传炸弹游戏
					/guess_number_game - 开始猜数字游戏
					""".trimIndent()
			} else {
				"群组内帮助已关闭，请私聊机器人咨询。"
			}
		}
		true
	}
}