package moe.feng.aoba.dao.common

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import moe.feng.aoba.support.classOf
import moe.feng.aoba.support.toJson
import moe.feng.aoba.support.toObject
import moe.feng.common.kt.StringUtil
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class KVDatabase(private val fileName: String = "default.json") : TableProvider() {

	@Volatile
	private var saveTask: Deferred<Unit>? = null

	init {
		load()
	}

	protected fun intValue(key: String? = null, defaultValue: Int = 0)
			= IntProperty(key, defaultValue)

	protected fun longValue(key: String? = null, defaultValue: Long = 0L)
			= LongProperty(key, defaultValue)

	protected fun floatValue(key: String? = null, defaultValue: Float = .0F)
			= FloatProperty(key, defaultValue)

	protected fun booleanValue(key: String? = null, defaultValue: Boolean = false)
			= BooleanProperty(key, defaultValue)

	protected fun stringValue(key: String? = null, defaultValue: String? = null)
			= NullableStringProperty(key, defaultValue)

	protected fun <T : Any> modelValue(key: String? = null, defaultModelJson: String = "{}", typeClass: Class<T>)
			= ModelProperty(key, defaultModelJson, typeClass)

	protected fun <E : Any> listValue(key: String? = null, defaultModelJson: String = "[]", elementClass: Class<E>)
			= ListProperty(key, defaultModelJson, elementClass)

	protected inline fun <reified T : Any> modelValue(key: String? = null, defaultModelJson: String = "{}")
			= modelValue(key, defaultModelJson, classOf<T>())

	protected inline fun <reified E : Any> listValue(key: String? = null, defaultModelJson: String = "[]")
			= listValue(key, defaultModelJson, classOf<E>())

	fun load() {
		val file = File(fileName)
		if (file.isFile) {
			println(file.absolutePath)
			try {
				file.inputStream().let { BufferedInputStream(it) }.apply {
					table = readBytes().toString(StandardCharsets.UTF_8).toObject<TableProvider>().table
					close()
				}
			} catch (e : Exception) {
				e.printStackTrace()
			}
		}
	}

	suspend fun save() {
		val file = File(fileName)
		if (!file.isFile) {
			file.createNewFile()
		}
		println(file.absolutePath)
		try {
			file.outputStream().let { BufferedOutputStream(it) }.apply {
				write(mapOf("table" to table).toJson().toByteArray())
				close()
			}
		} catch (e : Exception) {
			e.printStackTrace()
		}
	}

	@Synchronized
	fun scheduleSave() {
		saveTask?.cancel()
		saveTask = async(CommonPool) { save() }
	}

	protected class IntProperty internal constructor(key: String? = null, defaultValue: Int = 0)
		: RawTypeProperty<Int>(key, defaultValue)

	protected class LongProperty internal constructor(key: String? = null, defaultValue: Long = 0L)
		: RawTypeProperty<Long>(key, defaultValue)

	protected class FloatProperty internal constructor(key: String? = null, defaultValue: Float = .0F)
		: RawTypeProperty<Float>(key, defaultValue)

	protected class BooleanProperty internal constructor(key: String? = null, defaultValue: Boolean = false)
		: RawTypeProperty<Boolean>(key, defaultValue)

	protected class NullableStringProperty internal constructor(key: String? = null, defaultValue: String? = null)
		: RawTypeProperty<String?>(key, defaultValue)

	protected open class RawTypeProperty<T> internal constructor(
			private val _key: String? = null,
			private val defaultValue: T
	) : ReadWriteProperty<KVDatabase, T> {

		private fun getKey(property: KProperty<*>): String = _key ?: StringUtil.toCamelCase(property.name)

		override fun getValue(thisRef: KVDatabase, property: KProperty<*>): T {
			return thisRef.table[getKey(property)] as? T ?: defaultValue
		}

		override fun setValue(thisRef: KVDatabase, property: KProperty<*>, value: T) {
			thisRef.table[getKey(property)] = value
			thisRef.scheduleSave()
		}

	}

	protected open class ModelProperty<T : Any> internal constructor(
			private val _key: String? = null,
			private val defaultModelJson: String,
			private val modelClass: Class<T>
	) : ReadWriteProperty<KVDatabase, T> {

		private fun getKey(property: KProperty<*>): String = _key ?: StringUtil.toCamelCase(property.name)

		override fun getValue(thisRef: KVDatabase, property: KProperty<*>): T {
			if (getKey(property) !in thisRef.table.keys || !modelClass.isInstance(thisRef.table[getKey(property)])) {
				thisRef.table[getKey(property)] =
						(thisRef.table[getKey(property)]?.toJson() ?: defaultModelJson).toObject(modelClass)
			}
			return thisRef.table[getKey(property)] as T
		}

		override fun setValue(thisRef: KVDatabase, property: KProperty<*>, value: T) {
			thisRef.table[getKey(property)] = value
			thisRef.scheduleSave()
		}

	}

	protected open class ListProperty<E : Any> internal constructor(
			private val _key: String? = null,
			private val defaultModelJson: String,
			private val elementClass: Class<E>
	) : ReadWriteProperty<KVDatabase, MutableList<E>> {

		private fun getKey(property: KProperty<*>): String = _key ?: StringUtil.toCamelCase(property.name)

		override fun getValue(thisRef: KVDatabase, property: KProperty<*>): MutableList<E> {
			if (getKey(property) !in thisRef.table.keys
					|| !List::class.java.isInstance(thisRef.table[getKey(property)])
					|| !elementClass.isInstance((thisRef.table[getKey(property)] as MutableList<*>)[0])) {
				thisRef.table[getKey(property)] =
						(thisRef.table[getKey(property)]?.toJson() ?: defaultModelJson)
								.toObject<MutableList<Any>>()
								.map { it.toJson().toObject(elementClass) }
			}
			return thisRef.table[getKey(property)] as MutableList<E>
		}

		override fun setValue(thisRef: KVDatabase, property: KProperty<*>, value: MutableList<E>) {
			thisRef.table[getKey(property)] = value
			thisRef.scheduleSave()
		}

	}

}