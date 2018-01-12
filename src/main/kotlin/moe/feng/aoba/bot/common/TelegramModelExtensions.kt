package moe.feng.aoba.bot.common

import org.telegram.telegrambots.api.objects.User

fun User.getDisplayName(): String = (firstName + " " + (lastName ?: "")).trim()