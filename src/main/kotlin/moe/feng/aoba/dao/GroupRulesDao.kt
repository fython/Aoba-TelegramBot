package moe.feng.aoba.dao

import moe.feng.aoba.dao.common.KVDatabase
import moe.feng.aoba.model.GroupRules

object GroupRulesDao : KVDatabase("group_rules", "basic") {

	var data by mutableListValue<GroupRules>()

}
