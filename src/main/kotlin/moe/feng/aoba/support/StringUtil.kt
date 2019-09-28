package moe.feng.common.kt

import org.apache.http.util.TextUtils

object StringUtil {

    @JvmStatic
    private val MARKDOWN_ESCAPE_CHARS = arrayOf("_", "*", "[", "`")

    @JvmOverloads @JvmStatic
    fun toCamelCase(string: String, firstCharCapitalize: Boolean = false): String {
        if (TextUtils.isEmpty(string)) {
            throw IllegalArgumentException("Empty string cannot be converted to camel case.")
        }
        val newString = buildString {
            val iterator = string.iterator()
            while (iterator.hasNext()) {
                val char = iterator.next()
                if (char == '_') {
                    if (iterator.hasNext()) {
                        append(iterator.next().toUpperCase())
                    }
                } else {
                    append(char)
                }
            }
        }
        newString[0].let(if (firstCharCapitalize) Char::toUpperCase else Char::toLowerCase)
        return newString
    }

    @JvmStatic
    fun toMarkdownSafe(string: String): String {
        var res = string
        for (ch in MARKDOWN_ESCAPE_CHARS) {
            if (ch in res) {
                res = res.replace(ch, "\\$ch")
            }
        }
        return res
    }

}