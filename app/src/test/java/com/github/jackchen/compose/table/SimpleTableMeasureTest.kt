package com.github.jackchen.compose.table

import org.junit.Assert
import org.junit.Test

class SimpleTableMeasureTest {
    open class View(
        val width: Int,
        val height: Int
    )

    private val viewCaching = mutableListOf<String>()

    private val viewFactory = { row: Int, column: Int ->
        val key = "$row:$column"
        if (viewCaching.contains(key)) {
            throw IllegalAccessException("view is already taken.")
        }
        viewCaching.add(key)
        View(
            width = 100,
            height = 100
        )
    }

    class Table(width: Int, height: Int, val rowCount: Int, val columnCount: Int) : View(width, height) {
        companion object {
            private const val ROW_MASK: Int = 0xFFFF
            private const val COLUMN_BIT_COUNT = 16
        }

        var children = mutableMapOf<Int, View>()

        private fun getPosition(row: Int, column: Int): Int {
            return (row shl COLUMN_BIT_COUNT) + column
        }

        fun addChild(row: Int, column: Int, child: View) {
            val position = getPosition(row, column)
            children[position] = child
        }

        fun getChild(row: Int, column: Int): View {
            val position = getPosition(row, column)
            return children[position] ?: throw NullPointerException("There is no child at row:$row column:$column")
        }

        fun output(
            horizontalScrollState: ScrollState,
            verticalScrollState: ScrollState
        ): String {
            val output = StringBuilder()
            for (row in verticalScrollState.firstVisibleItem until verticalScrollState.lastVisibleItem) {
                for (column in horizontalScrollState.firstVisibleItem until horizontalScrollState.lastVisibleItem) {
                    val child = getChild(row, column)
                    Assert.assertNotNull(child)
                    output.append(" Cell:$row $column")
                }
                output.append('\n')
            }
            return output.toString()
        }
    }

    private fun buildTable(): Table {
        val crossAxisMaxSize = 200
        val mainAxisMaxSize = 500
        return Table(
            width = crossAxisMaxSize,
            height = mainAxisMaxSize,
            rowCount = 20,
            columnCount = 6
        )
    }

    class ScrollState(
        var firstVisibleItem: Int = 0,
        var firstVisibleItemOffset: Int = 0,
        var lastVisibleItem: Int = 0,
    ) : Cloneable {

        public override fun clone(): ScrollState {
            val scrollState = super.clone() as ScrollState
            scrollState.firstVisibleItem = firstVisibleItem
            scrollState.firstVisibleItemOffset = firstVisibleItemOffset
            scrollState.lastVisibleItem = lastVisibleItem
            return scrollState
        }
    }

    private fun cleanTable(table: Table) {
        table.children.clear()
        viewCaching.clear()
    }

    private fun fillContent(
        table: Table,
        tableData: TableData<View>,
        horizontalScrollState: ScrollState,
        verticalScrollState: ScrollState,
    ) {
        scrollBy(table, tableData, horizontalScrollState, verticalScrollState, 0, 0)
    }

    private fun fillContentInternal(
        oldScrollState: ScrollState,
        scrollState: ScrollState,
        tableData: TableData<View>,
        currentRow: Int,
        itemCount: Int,
        size: Int,
        delta: Int,
        viewCreateCallback: (View) -> Unit
    ) {
        val minOffset = 0
        val firstVisibleItem = oldScrollState.firstVisibleItem
        val firstVisibleItemOffset = oldScrollState.firstVisibleItemOffset
        var currentFirstVisibleItem = firstVisibleItem
        var currentFirstVisibleItemOffset = firstVisibleItemOffset - delta
        var forwardIndex = firstVisibleItem
        var sizeUsed = -currentFirstVisibleItemOffset
        while (0 < currentFirstVisibleItem && minOffset > currentFirstVisibleItemOffset) {
            val view = viewFactory(currentRow, currentFirstVisibleItem - 1)
            viewCreateCallback(view)
            tableData.add(currentRow, currentFirstVisibleItem - 1, view)
            currentFirstVisibleItem--
            currentFirstVisibleItemOffset += view.height
        }
        if (0 == currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
            currentFirstVisibleItemOffset = 0
            sizeUsed = 0
        }
        var invisibles: MutableList<View>? = null
        while (forwardIndex < itemCount && sizeUsed < size) {
            val view = viewFactory(currentRow, forwardIndex)
            viewCreateCallback(view)
            sizeUsed += view.height
            forwardIndex++
            if (sizeUsed < minOffset) {
                currentFirstVisibleItem = forwardIndex
                currentFirstVisibleItemOffset -= view.height
                if (null == invisibles) {
                    invisibles = mutableListOf()
                }
                invisibles.add(view)
            } else {
                tableData.add(currentRow, forwardIndex - 1, view)
            }
        }
        if (sizeUsed < size) {
            //Move back.
            var delta = size - sizeUsed
            currentFirstVisibleItemOffset -= delta
            sizeUsed += delta
            while (0 < currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
                val view = invisibles?.lastOrNull() ?: viewFactory(currentRow, currentFirstVisibleItem - 1)
                viewCreateCallback(view)
                tableData.add(currentRow, currentFirstVisibleItem - 1, view)
                currentFirstVisibleItemOffset += view.height
                currentFirstVisibleItem--
            }
        }
        scrollState.firstVisibleItem = currentFirstVisibleItem
        scrollState.firstVisibleItemOffset = currentFirstVisibleItemOffset
        scrollState.lastVisibleItem = forwardIndex
    }

    private fun scrollBy(
        table: Table,
        tableData: TableData<View>,
        horizontalScrollState: ScrollState,
        verticalScrollState: ScrollState,
        deltaX: Int,
        deltaY: Int
    ) {
        cleanTable(table)
        val minOffset = 0
        val height = table.height
        val rowCount = table.rowCount
        val columnCount = table.columnCount
        val firstVisibleItem = verticalScrollState.firstVisibleItem
        val firstVisibleItemOffset = verticalScrollState.firstVisibleItemOffset
        var currentFirstVisibleItem = firstVisibleItem
        val tempHorizontalScrollState = horizontalScrollState.clone()
        var currentFirstVisibleItemOffset = firstVisibleItemOffset - deltaY
        var forwardIndex = firstVisibleItem
        var mainAxisUsed = -currentFirstVisibleItemOffset
        while (0 < currentFirstVisibleItem && minOffset > currentFirstVisibleItemOffset) {
            var maxViewHeight = 0
            fillContentInternal(
                oldScrollState = tempHorizontalScrollState,
                scrollState = horizontalScrollState,
                tableData = tableData,
                currentRow = currentFirstVisibleItem - 1,
                itemCount = columnCount,
                size = table.width,
                delta = deltaX
            ) { view ->
                maxViewHeight = maxViewHeight.coerceAtLeast(view.height)
            }
            currentFirstVisibleItem--
            currentFirstVisibleItemOffset += maxViewHeight
        }
        if (0 == currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
            currentFirstVisibleItemOffset = 0
            mainAxisUsed = 0
        }
        while (forwardIndex < rowCount && mainAxisUsed < height) {
            var maxViewHeight = 0
            fillContentInternal(
                oldScrollState = tempHorizontalScrollState,
                scrollState = horizontalScrollState,
                tableData = tableData,
                currentRow = forwardIndex,
                itemCount = columnCount,
                size = table.width,
                delta = deltaX
            ) { view ->
                maxViewHeight = maxViewHeight.coerceAtLeast(view.height)
            }
            mainAxisUsed += maxViewHeight
            forwardIndex++
            if (mainAxisUsed < minOffset) {
                currentFirstVisibleItem = forwardIndex
                currentFirstVisibleItemOffset -= maxViewHeight
            }
        }
        if (mainAxisUsed < height) {
            //Move back.
            var delta = height - mainAxisUsed
            currentFirstVisibleItemOffset -= delta
            mainAxisUsed += delta
            while (0 < currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
                var maxViewHeight = 0
                for (column in horizontalScrollState.firstVisibleItem until horizontalScrollState.lastVisibleItem) {
                    val view = tableData.get(currentFirstVisibleItem, column)
                    maxViewHeight = maxViewHeight.coerceAtLeast(view.height)
                }
                currentFirstVisibleItemOffset += maxViewHeight
                currentFirstVisibleItem--
            }
        }
        verticalScrollState.firstVisibleItem = currentFirstVisibleItem
        verticalScrollState.firstVisibleItemOffset = currentFirstVisibleItemOffset
        verticalScrollState.lastVisibleItem = forwardIndex

        for (row in verticalScrollState.firstVisibleItem until verticalScrollState.lastVisibleItem) {
            for (column in horizontalScrollState.firstVisibleItem until horizontalScrollState.lastVisibleItem) {
                val view = tableData.get(row, column)
                table.addChild(row, column, view)
            }
        }
    }

    /**
     * Simulate the list scrolls down.
     */
    @Test
    fun testTableMoveForward() {
        val tableLayout = buildTable()
        val tableData = TableData<View>()
        val verticalScrollState = ScrollState()
        val horizontalScrollState = ScrollState()
        fillContent(tableLayout, tableData, horizontalScrollState, verticalScrollState)
        Assert.assertEquals(tableLayout.children.size, 10)
        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -5, -5)
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 0)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 5)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 0)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 5)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -105, -105)
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 1)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 10)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 1)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 10)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -205, -205)
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 3)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 15)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 3)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 15)
    }

    /**
     * Simulate the list scrolls up.
     */
    @Test
    fun testTableMoveBackward() {
        val tableLayout = buildTable()
        val tableData = TableData<View>()
        val verticalScrollState = ScrollState()
        val horizontalScrollState = ScrollState()
        fillContent(tableLayout, tableData, horizontalScrollState, verticalScrollState)
        Assert.assertEquals(tableLayout.children.size, 10)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -200, -300)
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 1)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 100)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 2)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 100)
        println(tableLayout.output(horizontalScrollState, verticalScrollState))

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, 200, 500)
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 0)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 0)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 0)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 0)

        println(tableLayout.output(horizontalScrollState, verticalScrollState))
    }

    @Test
    fun testTableMoveBackwardToTop() {
        val tableLayout = buildTable()
        val tableData = TableData<View>()
        val verticalScrollState = ScrollState()
        val horizontalScrollState = ScrollState()
        fillContent(tableLayout, tableData, horizontalScrollState, verticalScrollState)
        Assert.assertEquals(tableLayout.children.size, 10)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -101, -101)
        println(tableLayout.output(horizontalScrollState, verticalScrollState))
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 1)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 1)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 1)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 1)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, 200, 200)
        println(tableLayout.output(horizontalScrollState, verticalScrollState))
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 0)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 0)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 0)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 0)
    }

    /**
     * Simulate the list scrolls down to the bottom.
     */
    @Test
    fun testTableMoveForwardToBottom() {
        val tableLayout = buildTable()
        val tableData = TableData<View>()
        val verticalScrollState = ScrollState()
        val horizontalScrollState = ScrollState()
        fillContent(tableLayout, tableData, horizontalScrollState, verticalScrollState)
        Assert.assertEquals(tableLayout.children.size, 10)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -350, -1450)
        println(tableLayout.output(horizontalScrollState, verticalScrollState))
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 3)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 50)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 14)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 50)

        scrollBy(tableLayout, tableData, horizontalScrollState, verticalScrollState, -100, -100)
        println(tableLayout.output(horizontalScrollState, verticalScrollState))
        Assert.assertEquals(horizontalScrollState.firstVisibleItem, 4)
        Assert.assertEquals(horizontalScrollState.firstVisibleItemOffset, 0)
        Assert.assertEquals(verticalScrollState.firstVisibleItem, 15)
        Assert.assertEquals(verticalScrollState.firstVisibleItemOffset, 0)
    }

}