package com.github.jackchen.compose.table.ui.builder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppDemo(
    navigator: DemoNavigator,
    demo: Demo,
    onItemClick: OnDemoItemClickListener
) {
    if (demo is DemoCategory) {
        CategoryDemoPage(
            navigator = navigator,
            category = demo,
            onItemClick = onItemClick
        )
    }
    if (demo is ComposableDemo) {
        DemoPage(navigator = navigator, demoItem = demo)
    }
}

@Composable
private fun CategoryDemoPage(
    navigator: DemoNavigator,
    category: DemoCategory,
    onItemClick: OnDemoItemClickListener
) {
    AppPage(title = category.title, navigator) {
        LazyColumn {
            items(category.demoList) { item ->
                CategoryListItem(navigator, item, onItemClick)
                Divider(color = Color.LightGray)
            }
        }
    }
}

@Composable
private fun CategoryListItem(
    navigator: DemoNavigator,
    demo: Demo,
    onItemClick: OnDemoItemClickListener
) {
    Row(modifier = Modifier.clickable(
        interactionSource = MutableInteractionSource(),
        indication = rememberRipple(color = Color.LightGray),
        onClick = {
            onItemClick.onClick(navigator, demo)
        }
    )) {
        Text(
            text = demo.title,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        )
        if (demo is DemoCategory) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                modifier = Modifier.align(Alignment.CenterVertically),
                tint = Color.LightGray,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun DemoPage(
    navigator: DemoNavigator,
    demoItem: ComposableDemo
) {
    AppPage(demoItem.title, navigator) {
        demoItem.demo.invoke()
    }
}

@Composable
private fun AppPage(
    title: String,
    navigator: DemoNavigator,
    block: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            if (navigator.isRoot()) {
                TopAppBar(
                    title = { Text(text = title) }
                )
            } else {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                            )
                        }
                    },
                    title = { Text(text = title) }
                )
            }
        }) { _->
        block()
    }
}

interface OnDemoItemClickListener {
    fun onClick(navigator: DemoNavigator, demo: Demo)
}