package moe.feng.aoba.dao

import moe.feng.aoba.dao.common.KVDatabase

object StatisticsDao : KVDatabase("statistics", "default") {

    var lastLaunchTime by longValue()

    var chooseCommand by intValue()
    var spaceCommand by intValue()
    var replaceCommand by intValue()

    var bombGame by intValue()
    var guessNumberGame by intValue()
    var minesweeperGame by intValue()

    var joinedGroups by mutableListValue<Long>()

}
