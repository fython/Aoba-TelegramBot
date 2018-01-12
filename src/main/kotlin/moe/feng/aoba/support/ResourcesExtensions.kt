package moe.feng.aoba.support

import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

operator fun ResourceBundle.get(key: String): String =
		this.getString(key).toByteArray(StandardCharsets.ISO_8859_1).toString(StandardCharsets.UTF_8)

fun <T> resourceBundle(name: String) = ResourcesExtensionsProperty<T>(name)

class ResourcesExtensionsProperty<in T>(private val name: String) : ReadOnlyProperty<T, ResourceBundle> {

	private var value: ResourceBundle? = null

	override fun getValue(thisRef: T, property: KProperty<*>): ResourceBundle {
		if (value == null) {
			value = ResourceBundle.getBundle(name)
		}
		return value ?: getValue(thisRef, property)
	}

}