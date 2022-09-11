package com.github.jackchen.compose.table.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.SubcomposeLayout

@Composable
fun <E> SimpleTable(
    modifier: Modifier = Modifier,
    content: SimpleTableScope<E>.() -> Unit
) {
    val simpleTableScope = SimpleTableScopeImpl<E>()
    simpleTableScope.apply(content)
    var scrollableState = remember {
        SimpleTableScrollState()
    }
    Layout(
        content = {
            // Header with the content
            SimpleTableContent(
                modifier = modifier.fillMaxWidth(),
                scrollState = scrollableState,
                scope = simpleTableScope,
                header = { column ->
                    simpleTableScope.headerFor(column)
                }
            ) { row, column ->
                simpleTableScope.contentFor(row, column)
            }
        },
        modifier = modifier,
        measurePolicy = simpleTableContentMeasurePolicy()
    )
}

@Composable
private fun <E> SimpleTableContent(
    modifier: Modifier = Modifier,
    scrollState: SimpleTableScrollState,
    scope: SimpleTableScopeImpl<E>,
    header: (Int) -> SimpleItemContent,
    content: (Int, Int) -> SimpleItemContent,
) {
    val restorableItemContent = wrapWithStateRestoration(content)
    val cachingItemContentFactory = remember { restorableItemContent }
    val restorableItemHeaderContent = wrapWithStateRestoration(header)
    val cachingItemHeaderContentFactory = remember { restorableItemHeaderContent }
    var tableHeaderData by remember { mutableStateOf(SimpleTableData(rowCapacity = 1)) }
    var tableData by remember { mutableStateOf(SimpleTableData()) }
    SubcomposeLayout(
        modifier = modifier
            .scrollable(
                state = scrollState,
                reverseDirection = true,
            )
            .clipToBounds()
            .then(scrollState.remeasurementModifier)
    ) { constraints ->
        val lazyMeasureItemProvider = SimpleTableMeasureItemProvider(
            scope = this,
            headerContentFactory = cachingItemHeaderContentFactory,
            itemContentFactory = cachingItemContentFactory
        ) { placeable ->
            SimpleTableMeasureItem(placeable)
        }
        tableHeaderData.clear()
        tableData.clear()
        val measureResult = measureTable(
            constraints = constraints,
            itemProvider = lazyMeasureItemProvider,
            scope = scope,
            scrollState = scrollState,
            tableHeaderData = tableHeaderData,
            tableData = tableData,
        )
        val crossAxisState = measureResult.crossAxisState
        val mainAxisState = measureResult.mainAxisState
        scrollState.crossAxisState = crossAxisState
        scrollState.mainAxisState = mainAxisState
        val tableHeaderMainAxisSize = scope.getTableHeaderMainAxisSize()
        layoutTable(
            constraints = constraints,
            measureResult = measureResult,
            scrollState = scrollState,
            tableHeaderMainAxisSize = tableHeaderMainAxisSize
        )
    }
}

/**
 * Actually a simple column measure policy
 */
fun simpleTableContentMeasurePolicy(): MeasurePolicy {
    return MeasurePolicy { measurables, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            var mainAxisUsed = 0
            measurables.forEach { measurable ->
                val placeable = measurable.measure(
                    constraints.copy(maxHeight = constraints.maxHeight - mainAxisUsed)
                )
                placeable.place(0, mainAxisUsed)
                mainAxisUsed += placeable.height
            }
        }
    }
}

private val DEFAULT_KEY_FACTORY: ((Int, Int) -> Any) = { row, column -> "Table $row $column" }

interface SimpleTableScope<E> {
    fun header(
        items: List<String>,
        block: @Composable (String, Int) -> Unit
    )

    fun items(
        items: List<E>,
        key: ((Int, Int) -> Any) = DEFAULT_KEY_FACTORY,
        block: @Composable (E, Int, Int) -> Unit
    )

    fun items(
        itemCount: Int,
        key: ((Int, Int) -> Any) = DEFAULT_KEY_FACTORY,
        block: @Composable (Int, Int) -> Unit
    )
}

class SimpleItemContent(val key: Any, val content: @Composable () -> Unit)

class SimpleTableScopeImpl<E> : SimpleTableScope<E> {
    private var internalContentKey: (Int, Int) -> Any = DEFAULT_KEY_FACTORY
    private var headerFactory: (Int) -> @Composable () -> Unit = { _ -> {} }
    private var contentFactory: (Int, Int) -> @Composable () -> Unit = { _, _ -> {} }
    private val internalContentCaching = mutableMapOf<Any, @Composable () -> Unit>()
    private var tableRowCount = 0
    private var tableHeaderCrossAxisSize: IntArray = IntArray(0)
    private var tableHeaderMainAxisSize = 0
    val columnCount get() = tableHeaderCrossAxisSize.size
    val rowCount get() = tableRowCount

    fun setTableHeaderCrossAxisSize(column: Int, headerSize: Int) {
        tableHeaderCrossAxisSize[column] = headerSize
    }

    fun getTableHeaderCrossAxisSize(column: Int): Int {
        return tableHeaderCrossAxisSize[column]
    }

    fun setTableHeaderMainAxisSize(size: Int) {
        this.tableHeaderMainAxisSize = size
    }

    fun getTableHeaderMainAxisSize(): Int {
        return tableHeaderMainAxisSize
    }

    private fun key(row: Int, column: Int): Any {
        return internalContentKey(row, column)
    }

    override fun header(
        items: List<String>,
        block: @Composable (String, Int) -> Unit
    ) {
        // Initial table content
        tableHeaderCrossAxisSize = IntArray(items.size)
        headerFactory = { column ->
            {
                block(items[column], column)
            }
        }
    }

    override fun items(
        items: List<E>,
        key: (Int, Int) -> Any,
        content: @Composable (E, Int, Int) -> Unit
    ) {
        internalContentKey = key
        // Initial table content
        tableRowCount = items.size
        contentFactory = { row, column ->
            {
                content(items[row], row, column)
            }
        }
    }

    fun headerFor(column: Int): SimpleItemContent {
        val key = "Header:$column"
        val content = headerFactory.invoke(column)
        return SimpleItemContent(
            key = key,
            content = content
        )
    }

    fun contentFor(row: Int, column: Int): SimpleItemContent {
        val key = key(row, column)
        val content = internalContentCaching[key] ?: contentFactory.invoke(row, column)
        return SimpleItemContent(
            key = key,
            content = content
        )
    }

    override fun items(
        itemCount: Int,
        key: (Int, Int) -> Any,
        content: @Composable (Int, Int) -> Unit
    ) {
        internalContentKey = key
        // Initial table content
        tableRowCount = itemCount
        contentFactory = { row, column ->
            {
                content(row, column)
            }
        }
    }
}

@Composable
fun wrapWithStateRestoration(block: (Int) -> SimpleItemContent): (Int) -> SimpleItemContent {
    return remember(block) {
        { column ->
            block(column)
        }
    }
}

@Composable
fun wrapWithStateRestoration(block: (Int, Int) -> SimpleItemContent): (Int, Int) -> SimpleItemContent {
    return remember(block) {
        { row, column ->
            block(row, column)
        }
    }
}