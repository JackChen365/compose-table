package com.github.jackchen.compose.table.library

import androidx.compose.runtime.Stable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints

@Stable
class SimpleTableMeasureItemProvider(
    val scope: SubcomposeMeasureScope,
    val headerContentFactory: (Int) -> SimpleItemContent,
    val itemContentFactory: (Int, Int) -> SimpleItemContent,
    val measuredItemFactory: (List<Placeable>) -> SimpleTableMeasureItem
) {
    private val childConstraints = Constraints(
        maxWidth = Constraints.Infinity,
        maxHeight = Constraints.Infinity
    )

    fun getAndMeasureHeader(column: Int): SimpleTableMeasureItem {
        val simpleItemContent = headerContentFactory.invoke(column)
        val placeables = scope.subcompose(simpleItemContent.key, simpleItemContent.content).map {
            it.measure(childConstraints)
        }
        return measuredItemFactory(placeables)
    }

    fun getAndMeasure(
        row: Int,
        column: Int,
        minWidth: Int = 0,
        minHeight: Int = 0
    ): SimpleTableMeasureItem {
        val simpleItemContent = itemContentFactory.invoke(row, column)
        val placeables = scope.subcompose(simpleItemContent.key, simpleItemContent.content).map {
            it.measure(childConstraints.copy(minWidth = minWidth, maxWidth = minWidth, minHeight = minHeight))
        }
        return measuredItemFactory(placeables)
    }
}