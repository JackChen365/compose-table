package com.github.jackchen.compose.table.library

import androidx.compose.foundation.MutatePriority
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import kotlin.math.abs

class SimpleTableScrollState(
    var headerState: SimpleTableState = SimpleTableState(),
    var crossAxisState: SimpleTableState = SimpleTableState(),
    var mainAxisState: SimpleTableState = SimpleTableState(),
) : SimpleScrollableState {
    var scrollToConsumed: Offset = Offset.Zero

    private val scrollableState = SimpleScrollableState {
        -onScroll(-it)
    }

    private fun onScroll(distance: Offset): Offset {
        if (Offset.Zero == distance) {
            return Offset.Zero
        }
        scrollToConsumed += distance
        if (abs(scrollToConsumed.x) > 0.5f || abs(scrollToConsumed.y) > 0.5f) {
            remeasurement.forceRemeasure()
        }
        scrollToConsumed = Offset.Zero
        // Consume all
        return distance
    }

    private lateinit var remeasurement: Remeasurement

    val remeasurementModifier = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@SimpleTableScrollState.remeasurement = remeasurement
        }
    }

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    override fun dispatchRawDelta(delta: Offset): Offset {
        return scrollableState.dispatchRawDelta(delta)
    }

    override suspend fun scroll(scrollPriority: MutatePriority, block: suspend SimpleScrollScope.() -> Unit) {
        return scrollableState.scroll(scrollPriority, block)
    }
}