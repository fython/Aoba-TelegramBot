package moe.feng.aoba.bot.common

import moe.feng.aoba.dao.GroupRulesDao
import org.telegram.telegrambots.api.objects.Chat
import org.telegram.telegrambots.api.objects.User

fun User.getDisplayName(): String = (firstName + " " + (lastName ?: "")).trim()

fun Chat.isAllowGame(): Boolean = GroupRulesDao.data.find { it.id == this.id }?.isAllowGame ?: false