package moe.feng.aoba.support

fun <T> MutableList<T>.setOrAdd(element: T) {
    val index = indexOf(element)
    if (index == -1) {
        add(element)
    } else {
        set(index, element)
    }
}

fun <T> MutableList<T>.setOrAddBy(element: T, keySelector: (T) -> Any) {
    val index = indexOfFirst { keySelector(it) == keySelector(element) }
    if (index == -1) {
        add(element)
    } else {
        set(index, element)
    }
}
