package moe.feng.aoba.support

import java.util.*

fun Random.nextInt(range: IntRange): Int {
	return range.start + nextInt(range.last - range.start)
}