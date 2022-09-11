package com.github.jackchen.compose.table.ui.builder

import androidx.compose.runtime.Composable

/**
 * Generic demo with a [title] that will be displayed in the list of demos.
 */
sealed class Demo(val title: String) {
    override fun toString() = title
}

class ComposableDemo(title: String, var demo: (@Composable () -> Unit)) : Demo(title)

class DemoCategory(
    title: String,
    private val demoListInternal: MutableList<Demo> = mutableListOf()
) : Demo(title) {

    val demoList: List<Demo> = demoListInternal

    fun category(title: String, block: DemoCategory.() -> Unit) {
        val categoryItem = DemoCategory(title)
        categoryItem.apply(block)
        demoListInternal.add(categoryItem)
    }

    fun demo(title: String, block: @Composable () -> Unit) {
        val demoItem = ComposableDemo(title, block)
        demoListInternal.add(demoItem)
    }
}
