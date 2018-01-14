package moe.feng.aoba.dao

import moe.feng.aoba.dao.common.KVDatabase

object StatisticsDao : KVDatabase("statistics.json") {

	var lastLaunchTime by longValue()

	var chooseCommand by intValue()
	var spaceCommand by intValue()
	var replaceCommand by intValue()

	var bombGame by intValue()
	var guessNumberGame by intValue()

	var joinedGroups by listValue<Long>()

}