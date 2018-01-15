package moe.feng.aoba.model

import moe.feng.aoba.support.nextInt
import java.util.*
import kotlin.math.min

data class MineMap(val seed: Long, val width: Int, val height: Int, val mineCount: Int) {

	var map: Array<IntArray> = Array(height) { IntArray(width) }
	operator fun component5(): Array<IntArray> = map

	init {
		initMapFromSeed()
		map.forEach { line ->
			line.forEach {
				print("$it ")
			}
			println()
		}
	}

	fun initMapFromSeed() {
		val random = Random(seed)
		map.forEach { it.fill(0) }
		val mines = mutableSetOf<Pair<Int, Int>>()
		while (mines.size < mineCount) {
			val pos = random.nextInt(0..height) to random.nextInt(0..width)
			if (pos !in mines) {
				mines += pos
			}
		}
		for ((x, y) in mines) {
			map[x][y] = 9
			for ((xOffset, yOffset) in (-1..1).flatMap { xOffset -> (-1..1).map { yOffset -> xOffset to yOffset } }) {
				if ((x + xOffset) in 0 until height && (y + yOffset) in 0 until width) {
					map[x + xOffset][y + yOffset] = min(9, map[x + xOffset][y + yOffset] + 1)
				}
			}
		}
	}

	companion object {

		@JvmStatic fun create(width: Int, height: Int, mineCount: Int) =
				MineMap(System.currentTimeMillis(), width, height, mineCount)

	}

}