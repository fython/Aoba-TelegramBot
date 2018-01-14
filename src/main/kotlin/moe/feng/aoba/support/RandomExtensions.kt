package moe.feng.aoba.support

import java.util.*

val GLOBAL_RANDOM = Random(System.currentTimeMillis())

fun Random.nextInt(range: IntRange): Int {
	return range.start + nextInt(range.last - range.start)
}

fun <T> Collection<T>.randomOne(): T {
	return this.elementAt(GLOBAL_RANDOM.nextInt(this.size))
}