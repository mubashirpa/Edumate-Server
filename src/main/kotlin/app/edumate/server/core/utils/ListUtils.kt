package app.edumate.server.core.utils

object ListUtils {
    fun <T> moveToIndex(
        list: MutableList<T>,
        index: Int,
        predicate: (T) -> Boolean,
    ) {
        val itemIndex = list.indexOfFirst(predicate)
        if (itemIndex != -1 && itemIndex != index && index <= list.size) {
            val item = list.removeAt(itemIndex)
            list.add(index, item)
        }
    }
}
