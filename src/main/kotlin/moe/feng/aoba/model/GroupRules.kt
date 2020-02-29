package moe.feng.aoba.model

data class GroupRules(
		val id: Long,
		var isAllowGame: Boolean = false
) {

	override fun equals(other: Any?): Boolean {
		return (other as? GroupRules)?.id == this.id
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + isAllowGame.hashCode()
		return result
	}

}
