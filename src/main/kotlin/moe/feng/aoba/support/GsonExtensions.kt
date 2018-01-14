package moe.feng.aoba.support

import com.google.gson.GsonBuilder

val GSON = GsonBuilder().create()

fun <T : Any> T.toJson(): String = GSON.toJson(this)

fun <T : Any> String.toObject(clazz: Class<T>): T = GSON.fromJson(this, clazz)

inline fun <reified T : Any> String.toObject(): T = this.toObject(T::class.java)