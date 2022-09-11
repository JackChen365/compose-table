package com.github.jackchen.compose.table

class TableData<E>(rowCapacity: Int = 10, columnCapacity: Int = 10) {
    private var columnCapacity = 0
    private var rowCapacity = 0
    private var elementData: Array<Array<Any?>?>

    init {
        this.rowCapacity = rowCapacity
        this.columnCapacity = columnCapacity
        this.elementData = Array(rowCapacity) {
            arrayOfNulls(columnCapacity)
        }
    }

    fun add(row: Int, column: Int, e: E?) {
        // Use while expression in case double the size is not enough for the table.
        while (row >= elementData.size) {
            rowCapacity *= 2
            growRow(rowCapacity)
        }
        var columns = elementData[row]
        if (null == columns) {
            columns = arrayOfNulls(columnCapacity)
            elementData[row] = columns
        }
        var columnSize = elementData[row]?.size ?: 0
        while (column >= columnSize) {
            columnCapacity *= 2
            growColumn(columnCapacity)
            columnSize = elementData[row]?.size ?: 0
        }
        elementData[row]?.set(column, e)
    }

    fun get(row: Int, column: Int): E {
        return elementData[row]?.get(column) as E
    }

    fun getOrNull(row: Int, column: Int): E? {
        return elementData[row]?.get(column) as? E
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

    fun getRowCount() = rowCapacity

    fun getColumnCount() = columnCapacity
}