package moe.feng.aoba.support

fun Char.isChinese(): Boolean = Character.UnicodeScript.of(codePoint()) == Character.UnicodeScript.HAN

fun Char.codePoint(): Int = toString().codePointAt(0)