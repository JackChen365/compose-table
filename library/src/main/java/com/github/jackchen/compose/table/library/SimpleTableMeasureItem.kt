package com.github.jackchen.compose.table.library

import androidx.compose.ui.layout.Placeable

class SimpleTableMeasureItem(
    private val placeables: List<Placeable>
) {
    val crossAxisSize: Int
    val mainAxisSize: Int

    init {
        var maxHeight = 0
        var maxWidth = 0
        placeables.forEach { placeable ->
            maxHeight += placeable.height
            maxWidth = placeable.width.coerceAtLeast(maxWidth)
        }
        crossAxisSize = maxWidth
        mainAxisSize = maxHeight
    }

    fun place(
        scope: Placeable.PlacementScope,
        crossAxisOffset: Int,
        mainAxisOffset: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ) = with(scope) {
        for (i in placeables.indices) {
            val placeable = placeables[i]
            if (crossAxisOffset < layoutWidth && mainAxisOffset < layoutHeight) {
                placeable.place(crossAxisOffset, mainAxisOffset)
            }
        }
    }
}