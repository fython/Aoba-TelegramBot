package moe.feng.aoba.support

import kotlin.math.max
import kotlin.math.min

fun Int.limitIn(range: IntRange): Int = min(max(range.first, this), range.last)