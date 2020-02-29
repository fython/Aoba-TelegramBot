package moe.feng.aoba.dao

import moe.feng.aoba.dao.common.KVDatabase
import moe.feng.aoba.model.GroupRules
import moe.feng.aoba.support.setOrAdd

object GroupRulesDao : KVDatabase("group_rules", "basic") {

	var data by mutableListValue(Array<GroupRules>::class.java)

	private fun clearDuplicatedData() {
		data = data.distinctBy { it.id }.toMutableList()
	}

	fun isAllowGame(chatId: Long): Boolean {
		return data.find { it.id == chatId }?.isAllowGame == true
	}

	fun setAllowGame(chatId: Long, allow: Boolean) {
		clearDuplicatedData()
		val groupRules = data.find { it.id == chatId } ?: GroupRules(chatId)
		groupRules.isAllowGame = allow
		data.setOrAdd(groupRules)
	}

}
