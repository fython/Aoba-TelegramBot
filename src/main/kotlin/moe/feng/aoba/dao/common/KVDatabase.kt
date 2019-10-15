package moe.feng.aoba.dao.common

import com.google.gson.JsonObject
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import moe.feng.aoba.dao.GroupRulesDao
import moe.feng.aoba.dao.StatisticsDao
import moe.feng.aoba.model.GroupRules
import moe.feng.aoba.support.parseToList
import moe.feng.aoba.support.toJson
import moe.feng.aoba.support.parseToObject
import moe.feng.common.kt.StringUtil
import org.bson.Document
import java.io.File
import java.util.logging.Logger
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class KVDatabase(val databaseName: String, val tableName: String) {

    private val database: MongoDatabase = getDatabase(databaseName)
    private val collection = database.getCollection("kv_$tableName")

    private fun getItemByKey(key: String): Document? {
        return collection.find(eq("key", key)).firstOrNull()
    }

    private fun putItem(key: String, value: Any?) {
        if (key in this) {
            collection.updateOne(eq("key", key), set("value", value))
        } else {
            collection.insertOne(Document().append("key", key).append("value", value))
        }
    }

    fun delete(key: String): Boolean {
        return collection.deleteOne(eq("key", key)).deletedCount > 0
    }

    operator fun contains(key: String): Boolean {
        return collection.countDocuments(eq("key", key)) > 0
    }

    @JvmOverloads
    fun getString(key: String, defValue: String? = null): String? {
        return getItemByKey(key)?.getString("value") ?: defValue
    }

    fun putString(key: String, value: String?) {
        putItem(key, value)
    }

    @JvmOverloads
    fun getInt(key: String, defValue: Int = 0): Int {
        return getItemByKey(key)?.getInteger("value") ?: defValue
    }

    fun putInt(key: String, value: Int) {
        putItem(key, value)
    }

    @JvmOverloads
    fun getDouble(key: String, defValue: Double = .0): Double {
        return getItemByKey(key)?.getDouble("value") ?: defValue
    }

    fun putDouble(key: String, value: Double) {
        putItem(key, value)
    }

    @JvmOverloads
    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return getItemByKey(key)?.getBoolean("value") ?: defValue
    }

    fun putBoolean(key: String, value: Boolean) {
        putItem(key, value)
    }

    @JvmOverloads
    fun getLong(key: String, defValue: Long = 0L): Long {
        return getItemByKey(key)?.getLong("value") ?: defValue
    }

    fun putLong(key: String, value: Long) {
        putItem(key, value)
    }

    internal fun stringValue(key: String? = null, defValue: String? = null): ReadWriteProperty<KVDatabase, String?> {
        return StringProperty(key, defValue)
    }

    internal fun intValue(key: String? = null, defValue: Int = 0): ReadWriteProperty<KVDatabase, Int> {
        return IntProperty(key, defValue)
    }

    internal fun doubleValue(key: String? = null, defValue: Double = .0): ReadWriteProperty<KVDatabase, Double> {
        return DoubleProperty(key, defValue)
    }

    internal fun booleanValue(key: String? = null, defValue: Boolean = false): ReadWriteProperty<KVDatabase, Boolean> {
        return BooleanProperty(key, defValue)
    }

    internal fun longValue(key: String? = null, defValue: Long = 0L): ReadWriteProperty<KVDatabase, Long> {
        return LongProperty(key, defValue)
    }

    internal fun <E : Any> listValue(
            elementArrayClass: Class<Array<E>>,
            key: String? = null,
            defModelJson: String = "[]"
    ): ReadWriteProperty<KVDatabase, List<E>> {
        return ListProperty(key, defModelJson, elementArrayClass)
    }

    internal inline fun <reified E : Any> listValue(
            key: String? = null,
            defModelJson: String = "[]"
    ): ReadWriteProperty<KVDatabase, List<E>> {
        return listValue(Array<E>::class.java, key, defModelJson)
    }

    internal fun <E : Any> mutableListValue(
            elementArrayClass: Class<Array<E>>,
            key: String? = null,
            defModelJson: String = "[]"
    ): ReadWriteProperty<KVDatabase, MutableList<E>> {
        return MutableListProperty(key, defModelJson, elementArrayClass)
    }

    internal inline fun <reified E : Any> mutableListValue(
            key: String? = null,
            defModelJson: String = "[]"
    ): ReadWriteProperty<KVDatabase, MutableList<E>> {
        return mutableListValue(Array<E>::class.java, key, defModelJson)
    }

    internal fun <E : Any> modelValue(
            elementClass: Class<E>,
            key: String? = null,
            defModelJson: String = "{}"
    ): ReadWriteProperty<KVDatabase, E> {
        return ModelProperty(key, defModelJson, elementClass)
    }

    internal inline fun <reified E : Any> modelValue(
            key: String? = null,
            defModelString: String = "{}"
    ): ReadWriteProperty<KVDatabase, E> {
        return modelValue(E::class.java, key, defModelString)
    }

    private abstract class BaseProperty<T>(private val keyName: String?)
        : ReadWriteProperty<KVDatabase, T> {

        private fun getKey(property: KProperty<*>): String {
            return keyName ?: StringUtil.toCamelCase(property.name)
        }

        override fun getValue(thisRef: KVDatabase, property: KProperty<*>): T {
            return getValue(thisRef, getKey(property))
        }

        abstract fun getValue(thisRef: KVDatabase, key: String): T

        override fun setValue(thisRef: KVDatabase, property: KProperty<*>, value: T) {
            setValue(thisRef, getKey(property), value)
        }

        abstract fun setValue(thisRef: KVDatabase, key: String, value: T)

    }

    private class StringProperty(key: String?, val defValue: String?) : BaseProperty<String?>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): String? {
            return thisRef.getString(key, defValue)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: String?) {
            thisRef.putString(key, value)
        }

    }

    private class IntProperty internal constructor(key: String?, val defValue: Int): BaseProperty<Int>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): Int {
            return thisRef.getInt(key, defValue)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: Int) {
            thisRef.putInt(key, value)
        }

    }

    private class DoubleProperty internal constructor(key: String?, val defValue: Double): BaseProperty<Double>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): Double {
            return thisRef.getDouble(key, defValue)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: Double) {
            thisRef.putDouble(key, value)
        }

    }

    private class BooleanProperty internal constructor(key: String?, val defValue: Boolean): BaseProperty<Boolean>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): Boolean {
            return thisRef.getBoolean(key, defValue)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: Boolean) {
            thisRef.putBoolean(key, value)
        }

    }

    private class LongProperty internal constructor(key: String?, val defValue: Long): BaseProperty<Long>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): Long {
            return thisRef.getLong(key, defValue)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: Long) {
            thisRef.putLong(key, value)
        }

    }

    private class ModelProperty<E : Any> internal constructor(
            key: String?,
            val defModelJson: String,
            val modelClass: Class<E>
    ) : BaseProperty<E>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): E {
            val result = thisRef.getString(key) ?: defModelJson
            return result.parseToObject(modelClass)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: E) {
            thisRef.putString(key, value.toString())
        }

    }

    private class ListProperty<E : Any> internal constructor(
            key: String?,
            val defModelJson: String,
            val elementArrayClass: Class<Array<E>>
    ) : BaseProperty<List<E>>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): List<E> {
            val result = thisRef.getString(key) ?: defModelJson
            return result.parseToList(elementArrayClass)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: List<E>) {
            thisRef.putString(key, value.toJson())
        }

    }

    private class MutableListProperty<E : Any> internal constructor(
            key: String?,
            val defModelJson: String,
            val elementArrayClass: Class<Array<E>>
    ) : BaseProperty<MutableList<E>>(key) {

        override fun getValue(thisRef: KVDatabase, key: String): MutableList<E> {
            val result = thisRef.getString(key) ?: defModelJson
            return KVDBMutableList(thisRef, result.parseToList(elementArrayClass), key)
        }

        override fun setValue(thisRef: KVDatabase, key: String, value: MutableList<E>) {
            thisRef.putString(key, value.toJson())
        }

        private class KVDBMutableList<E>(
                private val db: KVDatabase,
                originalData: List<E>,
                private val key: String
        ) : MutableList<E> {

            private val data: MutableList<E> = originalData.toMutableList()

            override val size: Int
                get() = data.size

            fun commit() {
                db.putString(key, this.toJson())
            }

            override fun contains(element: E): Boolean {
                return data.contains(element)
            }

            override fun containsAll(elements: Collection<E>): Boolean {
                return data.containsAll(elements)
            }

            override fun get(index: Int): E {
                return data[index]
            }

            override fun indexOf(element: E): Int {
                return data.indexOf(element)
            }

            override fun isEmpty(): Boolean {
                return data.isEmpty()
            }

            override fun iterator(): MutableIterator<E> {
                return data.iterator()
            }

            override fun lastIndexOf(element: E): Int {
                return data.lastIndexOf(element)
            }

            override fun add(element: E): Boolean {
                try {
                    return data.add(element)
                } finally {
                    commit()
                }
            }

            override fun add(index: Int, element: E) {
                try {
                    return data.add(index, element)
                } finally {
                    commit()
                }
            }

            override fun addAll(index: Int, elements: Collection<E>): Boolean {
                try {
                    return data.addAll(index, elements)
                } finally {
                    commit()
                }
            }

            override fun addAll(elements: Collection<E>): Boolean {
                try {
                    return data.addAll(elements)
                } finally {
                    commit()
                }
            }

            override fun clear() {
                try {
                    return data.clear()
                } finally {
                    commit()
                }
            }

            override fun listIterator(): MutableListIterator<E> {
                return data.listIterator()
            }

            override fun listIterator(index: Int): MutableListIterator<E> {
                return data.listIterator(index)
            }

            override fun remove(element: E): Boolean {
                val removed = data.remove(element)
                if (removed) {
                    commit()
                }
                return removed
            }

            override fun removeAll(elements: Collection<E>): Boolean {
                val removed = data.removeAll(elements)
                if (removed) {
                    commit()
                }
                return removed
            }

            override fun removeAt(index: Int): E {
                val item = data.removeAt(index)
                if (item != null) {
                    commit()
                }
                return item
            }

            override fun retainAll(elements: Collection<E>): Boolean {
                val retained = data.retainAll(elements)
                if (retained) {
                    commit()
                }
                return retained
            }

            override fun set(index: Int, element: E): E {
                try {
                    return data.set(index, element)
                } finally {
                    commit()
                }
            }

            override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
                return data.subList(fromIndex, toIndex)
            }

        }

    }

    companion object {

        private lateinit var mongoClient: MongoClient
        private val databaseInstances: MutableMap<String, MongoDatabase> = mutableMapOf()

        @JvmStatic
        fun init() {
            mongoClient = MongoClients.create()
            importOldDataFromJson()
        }

        @JvmStatic
        fun importOldDataFromJson() {
            val logger = Logger.getLogger("KVDatabase")
            val groupRulesFile = File("group_rules.json")
            val statisticsFile = File("statistics.json")
            if (groupRulesFile.isFile) {
                logger.info("Importing old data from json: ${groupRulesFile.absolutePath}")
                try {
                    val json = groupRulesFile.readText()
                    val table = json.parseToObject<JsonObject>()["table"]!!.asJsonObject
                    table["data"]?.toJson()
                            ?.parseToList<GroupRules>()
                            ?.toMutableList()
                            ?.let { GroupRulesDao.data = it }
                    logger.info("Finished importing from json: ${groupRulesFile.absolutePath}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // groupRulesFile.renameTo(File("group_rules.imported.json"))
            }
            if (statisticsFile.isFile) {
                logger.info("Importing old data from json: ${statisticsFile.absolutePath}")
                try {
                    val json = statisticsFile.readText()
                    val table = json.parseToObject<JsonObject>()["table"]!!.asJsonObject
                    with(StatisticsDao) {
                        table["bombGame"]?.asInt?.let { bombGame = it }
                        table["chooseCommand"]?.asInt?.let { chooseCommand = it }
                        table["guessNumberGame"]?.asInt?.let { guessNumberGame = it }
                        table["joinedGroups"]?.toJson()
                                ?.parseToList<Long>()
                                ?.toMutableList()
                                ?.let { joinedGroups = it }
                        table["lastLaunchTime"]?.asLong?.let { lastLaunchTime = it }
                        table["minesweeperGame"]?.asInt?.let { minesweeperGame = it }
                        table["replaceCommand"]?.asInt?.let { replaceCommand = it }
                        table["spaceCommand"]?.asInt?.let { spaceCommand = it }
                    }
                    logger.info("Finished importing from json: ${statisticsFile.absolutePath}")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // statisticsFile.renameTo(File("statistics.imported.json"))
            }
        }

        private fun getDatabase(name: String): MongoDatabase {
            if (name !in databaseInstances) {
                databaseInstances[name] = mongoClient.getDatabase(name)
            }
            return databaseInstances[name] ?: throw IllegalStateException("Cannot get $name database")
        }

    }

}
