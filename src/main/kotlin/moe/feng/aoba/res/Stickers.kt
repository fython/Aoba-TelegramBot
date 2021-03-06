package moe.feng.aoba.res

import moe.feng.aoba.support.get
import moe.feng.aoba.support.resourceBundle
import moe.feng.aoba.support.json.parseToObject
import org.telegram.telegrambots.meta.api.objects.stickers.Sticker
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object Stickers {

	private val stickersRes by resourceBundle("stickers")

	val catWithClock by sticker()
	val killCat by sticker()
	val konataShoot by sticker()
	val toggle by sticker()
	val munikoQuestion by sticker()
	val invertedQuestion by sticker()

	private fun sticker(name: String? = null) = StickerProperty(name)

	private class StickerProperty(private val name: String? = null): ReadOnlyProperty<Stickers, Sticker> {

		private var value: Sticker? = null

		override fun getValue(thisRef: Stickers, property: KProperty<*>): Sticker {
			if (value == null) {
				value = thisRef.stickersRes[name ?: property.name].parseToObject()
			}
			return value ?: getValue(thisRef, property)
		}

	}

}
