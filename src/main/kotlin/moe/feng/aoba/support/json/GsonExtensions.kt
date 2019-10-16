package moe.feng.aoba.support.json

import com.google.gson.GsonBuilder

val GSON = GsonBuilder()
        .registerTypeAdapter(Long::class.java, LongJsonSerializer)
        .create()

fun <T : Any> T.toJson(): String = GSON.toJson(this)

fun <T : Any> String.parseToObject(clazz: Class<T>): T {
    return GSON.fromJson(this, clazz)
}

inline fun <reified T : Any> String.parseToObject(): T {
    return this.parseToObject(T::class.java)
}

fun <T : Any> String.parseToList(arrayClass: Class<Array<T>>): List<T> {
    return GSON.fromJson(this, arrayClass).toList()
}

inline fun <reified T : Any> String.parseToList(): List<T> {
    return this.parseToList(Array<T>::class.java)
}
