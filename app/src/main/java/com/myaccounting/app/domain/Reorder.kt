package com.myaccounting.app.domain

/** Port of src/utils/reorder.ts */
object Reorder {
    fun <T> reorderArray(list: List<T>, fromIndex: Int, toIndex: Int): List<T> {
        if (fromIndex < 0 || fromIndex >= list.size || toIndex < 0 || toIndex >= list.size) return list
        val result = list.toMutableList()
        val removed = result.removeAt(fromIndex)
        result.add(toIndex, removed)
        return result
    }

    fun <T> moveItem(list: List<T>, index: Int, up: Boolean): List<T> =
        reorderArray(list, index, if (up) index - 1 else index + 1)
}
