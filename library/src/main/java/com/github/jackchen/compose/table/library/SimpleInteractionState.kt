package com.github.jackchen.compose.table.library

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class SimpleInteractionState : State<Set<SimpleInteraction>> {

    private var map: Map<SimpleInteraction, Offset?> by mutableStateOf(emptyMap())

    override val value: Set<SimpleInteraction>
        get() = map.keys

    fun addInteraction(interaction: SimpleInteraction, position: Offset? = null) {
        if (interaction !in this) map = map + (interaction to position)
    }

    fun removeInteraction(interaction: SimpleInteraction) {
        if (interaction in this) map = map - interaction
    }

    operator fun contains(interaction: SimpleInteraction): Boolean = map.contains(interaction)
}
