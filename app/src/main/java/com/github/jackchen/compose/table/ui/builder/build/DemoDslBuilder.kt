package com.github.jackchen.compose.table.ui.builder.build

import androidx.compose.runtime.Composable
import com.github.jackchen.compose.table.ui.builder.ComposableDemo
import com.github.jackchen.compose.table.ui.builder.Demo
import com.github.jackchen.compose.table.ui.builder.DemoCategory

class DemoDslBuilder : DemoBuilder<DemoDslBuilder.DemoScope.() -> Unit> {
    override fun buildDemoList(block: DemoScope.() -> Unit): List<Demo> {
        val demoScope = DemoScope()
        demoScope.apply(block)
        return demoScope.getCategoryList()
    }

    class DemoScope {
        private val categoryInternalList: MutableList<Demo> = mutableListOf()

        fun getCategoryList(): List<Demo> = categoryInternalList

        fun demo(title: String, block: @Composable () -> Unit) {
            val demoItem = ComposableDemo(title, block)
            categoryInternalList.add(demoItem)
        }

        fun category(title: String, block: DemoCategory.() -> Unit) {
            val categoryItem = DemoCategory(title)
            categoryItem.apply(block)
            categoryInternalList.add(categoryItem)
        }
    }
}