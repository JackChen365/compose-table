package com.github.jackchen.compose.table.ui.builder

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import com.github.jackchen.compose.table.R
import com.github.jackchen.compose.table.ui.builder.build.ComposableDemoBuilder
import com.github.jackchen.compose.table.ui.builder.build.DemoDslBuilder
import java.util.*

/**
 * Build the demo list by DSL
 */
fun buildAppDemo(block: DemoDslBuilder.DemoScope.() -> Unit): List<Demo> {
    val builder = DemoDslBuilder()
    return builder.buildDemoList(block)
}

/**
 * Build the demo list by composable annotation
 */
fun buildComposableDemoList(vararg classArray: Class<*>): List<Demo> {
    val builder = ComposableDemoBuilder()
    return builder.buildDemoList(classArray.toList())
}

fun ComponentActivity.initialAndDisplayDemoList(block: DemoDslBuilder.DemoScope.() -> Unit) {
    val demoList = buildAppDemo(block)
    val appName = getString(R.string.app_name)
    val rootCategory = DemoCategory(appName, demoList.toMutableList())
    buildActivityDemoList(rootCategory)
}

fun ComponentActivity.initialAndDisplayComposeClassDemoList(vararg classArray: Class<*>) {
    val demoList = buildComposableDemoList(*classArray)
    val appName = getString(R.string.app_name)
    val rootCategory = DemoCategory(appName, demoList.toMutableList())
    buildActivityDemoList(rootCategory)
}

private fun ComponentActivity.buildActivityDemoList(rootCategory: DemoCategory) {
    val itemClickListener = object : OnDemoItemClickListener {
        override fun onClick(navigator: DemoNavigator, demo: Demo) {
            //Going forward.
            setContent {
                navigator.navigateTo(demo)
                AppDemo(navigator, demo, this)
            }
        }
    }
    val demoNavigator = DemoNavigator(
        backDispatcher = onBackPressedDispatcher,
        rootDemo = rootCategory
    ) { navigator, demo ->
        //Going backward.
        setContent {
            AppDemo(navigator, demo, itemClickListener)
        }
    }
    setContent {
        //Initial the demo list
        demoNavigator.navigateTo(rootCategory)
        AppDemo(demoNavigator, rootCategory, itemClickListener)
    }
}

class DemoNavigator(
    private val backDispatcher: OnBackPressedDispatcher,
    private val rootDemo: Demo,
    private val backStack: ArrayDeque<Demo> = ArrayDeque(),
    private val onBackPressed: (DemoNavigator, Demo) -> Unit,
) {
    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            popBackStack()
        }
    }.apply {
        isEnabled = !isRoot()
        backDispatcher.addCallback(this)
    }

    fun isRoot() = backStack.peek() == rootDemo

    fun navigateTo(demo: Demo) {
        backStack.push(demo)
        onBackPressedCallback.isEnabled = true
    }

    fun popBackStack() {
        if (!backStack.isEmpty()) {
            backStack.pop()
            val demo = backStack.peek()
            if (null != demo) {
                onBackPressed(this, demo)
            }
        }
        onBackPressedCallback.isEnabled = !isRoot()
    }

}