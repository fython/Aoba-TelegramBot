package moe.feng.common.kt

import org.apache.http.util.TextUtils

object StringUtil {

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

}