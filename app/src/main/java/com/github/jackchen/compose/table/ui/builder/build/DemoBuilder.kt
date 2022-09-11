package com.github.jackchen.compose.table.ui.builder.build

import com.github.jackchen.compose.table.ui.builder.Demo

interface DemoBuilder<T> {
    fun buildDemoList(t: T): List<Demo>
}