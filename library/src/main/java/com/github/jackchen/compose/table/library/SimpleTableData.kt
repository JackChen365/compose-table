package com.github.jackchen.compose.table.library

import java.util.*

class SimpleTableData(rowCapacity: Int = 40, columnCapacity: Int = 10) {
    companion object {
        private const val UN_SET = -1
    }

    private var columnCapacity = 0
    private var rowCapacity = 0
    private var internalIndex: Int = UN_SET
    private var baseline: Int = UN_SET
    private var elementData: Array<Array<SimpleTableMeasureItem?>?>

    init {
        this.rowCapacity = rowCapacity
        this.columnCapacity = columnCapacity
        this.elementData = arrayOfNulls(rowCapacity)
        internalIndex = elementData.size / 2
    }

    fun add(row: Int, column: Int, e: SimpleTableMeasureItem?) {
        if (baseline == UN_SET) {
            baseline = row
        }
        // Use while expression in case double the size is not enough for the table.
        val index = internalIndex + row - baseline
        while (index >= elementData.size) {
            rowCapacity *= 2
            growRow(rowCapacity)
        }
        var columns = elementData[index]
        if (null == columns) {
            columns = arrayOfNulls(columnCapacity)
            elementData[index] = columns
        }
        var columnSize = elementData[index]?.size ?: 0
        while (column >= columnSize) {
            columnCapacity *= 2
            growColumn(columnCapacity)
            columnSize = elementData[index]?.size ?: 0
        }
        elementData[index]?.set(column, e)
    }

    fun get(row: Int, column: Int): SimpleTableMeasureItem {
        val index = internalIndex + row - baseline
        return elementData[index]?.get(column) as SimpleTableMeasureItem
    }

    fun getOrNull(row: Int, column: Int): SimpleTableMeasureItem? {
        val index = internalIndex + row - baseline
        return elementData[index]?.getOrNull(column)
    }

    fun get(row: Int): Array<SimpleTableMeasureItem?>? {
        val index = internalIndex + row - baseline
        return elementData[index]
    }

    private fun growRow(capacity: Int) {
        elementData = elementData.copyOf(capacity)
    }

    private fun growColumn(capacity: Int) {
        for (i in elementData.indices) {
            val array = elementData[i]
            if (null != array) {
                elementData[i] = array.copyOf(capacity)
            }
        }
    }

    fun clear() {
        baseline = UN_SET
        internalIndex = elementData.size / 2
        for (i in elementData.indices) {
            val array = elementData[i]
            if (null != array) {
                Arrays.fill(array, null)
            }
        }
    }
}