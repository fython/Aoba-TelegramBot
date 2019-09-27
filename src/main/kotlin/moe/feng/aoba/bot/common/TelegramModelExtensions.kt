package moe.feng.aoba.bot.common

import moe.feng.aoba.dao.GroupRulesDao
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.User

fun String.toMarkdownSafeText(): String {
    return this.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("`", "\\`")
}

fun User.getDisplayName(): String = (firstName + " " + (lastName ?: "")).trim()

fun User.toMentionText(): String {
    if (this.userName != null) {
        return "@$userName"
    } else {
        return "[${getDisplayName()}](tg://user?id=$id)"
    }
}

fun Chat.isAllowGame(): Boolean = GroupRulesDao.data.find { it.id == this.id }?.isAllowGame ?: false