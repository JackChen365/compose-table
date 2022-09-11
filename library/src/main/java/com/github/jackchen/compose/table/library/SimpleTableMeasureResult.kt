package com.github.jackchen.compose.table.library

class SimpleTableMeasureResult(
    val tableHeaderData: SimpleTableData,
    val tableData: SimpleTableData,
    val crossAxisState: SimpleTableState,
    val mainAxisState: SimpleTableState,
)

class SimpleTableState(
    var firstVisibleItem: Int = 0,
    var firstVisibleItemOffset: Int = 0,
    var lastVisibleItem: Int = 0,
)