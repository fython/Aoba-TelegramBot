package moe.feng.aoba.support.json

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.logging.Logger

object LongJsonSerializer : JsonSerializer<Long> {

    override fun serialize(src: Long?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        var value = BigDecimal.valueOf(src ?: 0L)
        try {
            value = BigDecimal(value.toBigIntegerExact())
        } catch (ignored: ArithmeticException) {

        }
        Logger.getGlobal().info("value=$value")
        return JsonPrimitive(value)
    }

}
