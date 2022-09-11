package com.github.jackchen.compose.table.library

import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

private const val HEADER_ROW = 0

fun <E> measureTable(
    constraints: Constraints,
    itemProvider: SimpleTableMeasureItemProvider,
    scope: SimpleTableScopeImpl<E>,
    scrollState: SimpleTableScrollState,
    tableHeaderData: SimpleTableData,
    tableData: SimpleTableData,
): SimpleTableMeasureResult {
    val totalRowCount = scope.rowCount
    val totalColumnCount = scope.columnCount
    val crossAxisMeasureState = scrollState.crossAxisState
    val mainAxisMeasureState = scrollState.mainAxisState
    measureTableHeader(
        constraints = constraints,
        itemProvider = itemProvider,
        scope = scope,
        scrollState = scrollState,
        tableData = tableHeaderData
    )
    if (0 >= totalRowCount || 0 >= totalColumnCount) {
        return SimpleTableMeasureResult(
            tableData = tableData,
            tableHeaderData = tableHeaderData,
            crossAxisState = crossAxisMeasureState,
            mainAxisState = mainAxisMeasureState
        )
    }
    val tableHeaderMainAxisSize = scope.getTableHeaderMainAxisSize()
    val maxHeight = constraints.maxHeight - tableHeaderMainAxisSize
    measureTableVertically(
        constraints = constraints,
        itemProvider = itemProvider,
        scope = scope,
        scrollState = scrollState,
        tableData = tableData,
        maxHeight = maxHeight
    )
    return SimpleTableMeasureResult(
        tableData = tableData,
        tableHeaderData = tableHeaderData,
        crossAxisState = crossAxisMeasureState,
        mainAxisState = mainAxisMeasureState
    )
}

fun <E> measureTableHeader(
    constraints: Constraints,
    itemProvider: SimpleTableMeasureItemProvider,
    scope: SimpleTableScopeImpl<E>,
    scrollState: SimpleTableScrollState,
    tableData: SimpleTableData,
) {
    val columnCount = scope.columnCount
    val headerState = scrollState.headerState
    val crossAxisState = scrollState.crossAxisState
    val delta = scrollState.scrollToConsumed

    val firstColumnVisibleItem = crossAxisState.firstVisibleItem
    val firstColumnVisibleItemOffset = crossAxisState.firstVisibleItemOffset
    var maxMainAxisSize = 0
    measureTableHorizontally(
        firstVisibleItem = firstColumnVisibleItem,
        firstVisibleItemOffset = firstColumnVisibleItemOffset,
        tableState = headerState,
        tableData = tableData,
        currentRow = HEADER_ROW,
        itemCount = columnCount,
        size = constraints.maxWidth,
        delta = delta.x.roundToInt()
    ) { column ->
        val measureItem = itemProvider.getAndMeasureHeader(column = column)
        maxMainAxisSize = maxMainAxisSize.coerceAtLeast(measureItem.mainAxisSize)
        scope.setTableHeaderCrossAxisSize(column, measureItem.crossAxisSize)
        measureItem
    }
    scope.setTableHeaderMainAxisSize(maxMainAxisSize)
}

fun MeasureScope.layoutTable(
    constraints: Constraints,
    measureResult: SimpleTableMeasureResult,
    scrollState: SimpleTableScrollState,
    tableHeaderMainAxisSize: Int
): MeasureResult {
    val headerState = scrollState.headerState
    val crossAxisState = scrollState.crossAxisState
    val mainAxisState = scrollState.mainAxisState
    val layoutWidth = constraints.maxWidth
    val layoutHeight = constraints.maxHeight
    val tableData = measureResult.tableData
    val tableHeaderData = measureResult.tableHeaderData
    return layout(layoutWidth, layoutHeight) {
        // Layout the content
        var mainAxisOffset = tableHeaderMainAxisSize - mainAxisState.firstVisibleItemOffset
        for (row in mainAxisState.firstVisibleItem until mainAxisState.lastVisibleItem) {
            var maxMainAxisSize = 0
            var crossAxisOffset = -crossAxisState.firstVisibleItemOffset
            for (column in crossAxisState.firstVisibleItem until crossAxisState.lastVisibleItem) {
                val measureItem = tableData.get(row, column)
                measureItem.place(
                    scope = this,
                    crossAxisOffset = crossAxisOffset,
                    mainAxisOffset = mainAxisOffset,
                    layoutWidth = layoutWidth,
                    layoutHeight = layoutHeight
                )
                crossAxisOffset += measureItem.crossAxisSize
                maxMainAxisSize = maxMainAxisSize.coerceAtLeast(measureItem.mainAxisSize)
            }
            mainAxisOffset += maxMainAxisSize
        }
        // Layout the header
        var headerMainAxisSizeOffset = -headerState.firstVisibleItemOffset
        for (column in headerState.firstVisibleItem until headerState.lastVisibleItem) {
            val measureItem = tableHeaderData.get(HEADER_ROW, column)
            measureItem.place(
                scope = this,
                crossAxisOffset = headerMainAxisSizeOffset,
                mainAxisOffset = 0,
                layoutWidth = layoutWidth,
                layoutHeight = layoutHeight
            )
            headerMainAxisSizeOffset += measureItem.crossAxisSize
        }
    }
}

private fun measureTableHorizontally(
    firstVisibleItem: Int,
    firstVisibleItemOffset: Int,
    tableState: SimpleTableState,
    tableData: SimpleTableData,
    currentRow: Int,
    itemCount: Int,
    size: Int,
    delta: Int,
    measuredItemFactory: (Int) -> SimpleTableMeasureItem
) {
    val minOffset = 0
    var currentFirstVisibleItem = firstVisibleItem
    var currentFirstVisibleItemOffset = firstVisibleItemOffset - delta
    var crossAxisUsed = -currentFirstVisibleItemOffset
    var forwardIndex = firstVisibleItem
    while (0 < currentFirstVisibleItem && minOffset > currentFirstVisibleItemOffset) {
        val measureItem = measuredItemFactory(currentFirstVisibleItem - 1)
        tableData.add(currentRow, currentFirstVisibleItem - 1, measureItem)
        currentFirstVisibleItem--
        currentFirstVisibleItemOffset += measureItem.crossAxisSize
    }
    if (0 == currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
        currentFirstVisibleItemOffset = 0
        crossAxisUsed = 0
    }
    while (forwardIndex < itemCount && crossAxisUsed < size) {
        val measureItem = measuredItemFactory(forwardIndex)
        tableData.add(currentRow, forwardIndex, measureItem)
        crossAxisUsed += measureItem.crossAxisSize
        forwardIndex++
        if (crossAxisUsed < minOffset) {
            currentFirstVisibleItem = forwardIndex
            currentFirstVisibleItemOffset -= measureItem.crossAxisSize
        }
    }
    if (crossAxisUsed < size) {
        //Move back.
        var delta = size - crossAxisUsed
        currentFirstVisibleItemOffset -= delta
        crossAxisUsed += delta
        while (0 < currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
            var measureItem = tableData.getOrNull(currentRow, currentFirstVisibleItem - 1)
            if (null == measureItem) {
                measureItem = measuredItemFactory(currentFirstVisibleItem - 1)
                tableData.add(currentRow, currentFirstVisibleItem - 1, measureItem)
            }
            currentFirstVisibleItemOffset += measureItem.crossAxisSize
            currentFirstVisibleItem--
        }
    }
    tableState.firstVisibleItem = currentFirstVisibleItem
    tableState.firstVisibleItemOffset = currentFirstVisibleItemOffset
    tableState.lastVisibleItem = forwardIndex
}

private fun <E> measureTableVertically(
    constraints: Constraints,
    itemProvider: SimpleTableMeasureItemProvider,
    scope: SimpleTableScopeImpl<E>,
    scrollState: SimpleTableScrollState,
    tableData: SimpleTableData,
    maxHeight: Int,
) {
    val minOffset = 0
    val rowCount = scope.rowCount
    val columnCount = scope.columnCount

    val crossAxisState = scrollState.crossAxisState
    val mainAxisState = scrollState.mainAxisState
    val delta = scrollState.scrollToConsumed

    val firstColumnVisibleItem = crossAxisState.firstVisibleItem
    val firstColumnVisibleItemOffset = crossAxisState.firstVisibleItemOffset

    val firstVisibleItem = mainAxisState.firstVisibleItem
    val firstVisibleItemOffset = mainAxisState.firstVisibleItemOffset
    var currentFirstVisibleItem = firstVisibleItem
    var currentFirstVisibleItemOffset = firstVisibleItemOffset - delta.y.toInt()
    var forwardIndex = firstVisibleItem
    var mainAxisUsed = -currentFirstVisibleItemOffset
    while (0 < currentFirstVisibleItem && minOffset > currentFirstVisibleItemOffset) {
        var maxViewHeight = 0
        measureTableHorizontally(
            firstVisibleItem = firstColumnVisibleItem,
            firstVisibleItemOffset = firstColumnVisibleItemOffset,
            tableState = crossAxisState,
            tableData = tableData,
            currentRow = currentFirstVisibleItem - 1,
            itemCount = columnCount,
            size = constraints.maxWidth,
            delta = delta.x.roundToInt()
        ) { column ->
            val tableHeaderSize = scope.getTableHeaderCrossAxisSize(column)
            val measureItem = itemProvider.getAndMeasure(
                row = currentFirstVisibleItem - 1,
                column = column,
                minWidth = tableHeaderSize
            )
            maxViewHeight = maxViewHeight.coerceAtLeast(measureItem.mainAxisSize)
            measureItem
        }
        currentFirstVisibleItem--
        currentFirstVisibleItemOffset += maxViewHeight
    }
    if (0 == currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
        currentFirstVisibleItemOffset = 0
        mainAxisUsed = 0
    }
    while (forwardIndex < rowCount && mainAxisUsed < maxHeight) {
        var maxViewHeight = 0
        measureTableHorizontally(
            firstVisibleItem = firstColumnVisibleItem,
            firstVisibleItemOffset = firstColumnVisibleItemOffset,
            tableState = crossAxisState,
            tableData = tableData,
            currentRow = forwardIndex,
            itemCount = columnCount,
            size = constraints.maxWidth,
            delta = delta.x.roundToInt()
        ) { column ->
            val tableHeaderSize = scope.getTableHeaderCrossAxisSize(column)
            val measureItem = itemProvider.getAndMeasure(
                row = forwardIndex,
                column = column,
                minWidth = tableHeaderSize
            )
            maxViewHeight = maxViewHeight.coerceAtLeast(measureItem.mainAxisSize)
            measureItem
        }
        mainAxisUsed += maxViewHeight
        forwardIndex++
        if (mainAxisUsed < minOffset) {
            currentFirstVisibleItem = forwardIndex
            currentFirstVisibleItemOffset -= maxViewHeight
        }
    }
    if (mainAxisUsed < maxHeight) {
        //Move back.
        var delta = maxHeight - mainAxisUsed
        currentFirstVisibleItemOffset -= delta
        mainAxisUsed += delta
        while (0 < currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
            var maxViewHeight = 0
            val columns = tableData.get(currentFirstVisibleItem)
            if (null == columns) {
                var maxViewHeight = 0
                measureTableHorizontally(
                    firstVisibleItem = firstColumnVisibleItem,
                    firstVisibleItemOffset = firstColumnVisibleItemOffset,
                    tableState = crossAxisState,
                    tableData = tableData,
                    currentRow = currentFirstVisibleItem,
                    itemCount = columnCount,
                    size = constraints.maxWidth,
                    delta = 0
                ) { column ->
                    val tableHeaderSize = scope.getTableHeaderCrossAxisSize(column)
                    val measureItem = itemProvider.getAndMeasure(
                        row = currentFirstVisibleItem,
                        column = column,
                        minWidth = tableHeaderSize
                    )
                    maxViewHeight = maxViewHeight.coerceAtLeast(measureItem.mainAxisSize)
                    measureItem
                }
                mainAxisUsed += maxViewHeight
            } else {
                for (column in crossAxisState.firstVisibleItem until crossAxisState.lastVisibleItem) {
                    val measureItem = tableData.get(currentFirstVisibleItem, column)
                    maxViewHeight = maxViewHeight.coerceAtLeast(measureItem.mainAxisSize)
                }
            }
            currentFirstVisibleItemOffset += maxViewHeight
            currentFirstVisibleItem--
        }
    }
    mainAxisState.firstVisibleItem = currentFirstVisibleItem
    mainAxisState.firstVisibleItemOffset = currentFirstVisibleItemOffset
    mainAxisState.lastVisibleItem = forwardIndex
}
