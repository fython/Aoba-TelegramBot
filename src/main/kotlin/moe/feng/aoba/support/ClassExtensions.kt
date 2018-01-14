package moe.feng.aoba.support

inline fun <reified T> classOf(): Class<T> = T::class.java