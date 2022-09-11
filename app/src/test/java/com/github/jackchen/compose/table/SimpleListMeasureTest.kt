package com.github.jackchen.compose.table

import org.junit.Assert
import org.junit.Test

class SimpleListMeasureTest {
    private val viewWidth = 200
    private val viewHeight = 100

    open class View(
        val width: Int,
        val height: Int
    )

    private val viewCaching = mutableListOf<Int>()

    private val viewFactory = { index: Int ->
        if (viewCaching.contains(index)) {
            throw IllegalAccessException("view is already taken.")
        }
        viewCaching.add(index)
        View(
            width = viewWidth,
            height = viewHeight
        )
    }

    class ListView(width: Int, height: Int, val itemCount: Int) : View(width, height) {
        var children = mutableListOf<View>()
    }

    private fun buildList(): ListView {
        val crossAxisMaxSize = 200
        val mainAxisMaxSize = 500
        return ListView(
            width = crossAxisMaxSize,
            height = mainAxisMaxSize,
            itemCount = 20
        )
    }

    private class ScrollState(
        var firstVisibleItem: Int = 0,
        var firstVisibleItemOffset: Int = 0
    )

    private fun cleanListView(listView: ListView) {
        listView.children.clear()
        viewCaching.clear()
    }

    private fun layoutChildren(listView: ListView) {
        cleanListView(listView)
        val height = listView.height
        var mainAxisUsed = 0
        var currentFirstIndex = 0
        while (currentFirstIndex < listView.itemCount && mainAxisUsed < height) {
            val view = viewFactory(currentFirstIndex)
            listView.children.add(view)
            mainAxisUsed += view.height
            currentFirstIndex++
        }
    }

    private fun scrollBy(listView: ListView, scrollState: ScrollState, deltaY: Int) {
        cleanListView(listView)
        val minOffset = 0
        val height = listView.height
        val firstVisibleItem = scrollState.firstVisibleItem
        val firstVisibleItemOffset = scrollState.firstVisibleItemOffset
        var currentFirstVisibleItem = firstVisibleItem
        var currentFirstVisibleItemOffset = firstVisibleItemOffset - deltaY
        var forwardIndex = firstVisibleItem
        var mainAxisUsed = -currentFirstVisibleItemOffset
        while (0 < currentFirstVisibleItem && minOffset > currentFirstVisibleItemOffset) {
            val view = viewFactory(currentFirstVisibleItem - 1)
            listView.children.add(0, view)
            currentFirstVisibleItem--
            currentFirstVisibleItemOffset += view.height
        }
        while (forwardIndex < listView.itemCount && mainAxisUsed < height) {
            val view = viewFactory(forwardIndex)
            listView.children.add(view)
            mainAxisUsed += view.height
            forwardIndex++
            if (mainAxisUsed < minOffset) {
                currentFirstVisibleItem = forwardIndex
                currentFirstVisibleItemOffset -= view.height
            }
        }
        scrollState.firstVisibleItem = currentFirstVisibleItem
        scrollState.firstVisibleItemOffset = currentFirstVisibleItemOffset
    }

    private fun scrollByAndCheckBoundary(listView: ListView, scrollState: ScrollState, deltaY: Int) {
        cleanListView(listView)
        val minOffset = 0
        val height = listView.height
        val firstVisibleItem = scrollState.firstVisibleItem
        val firstVisibleItemOffset = scrollState.firstVisibleItemOffset
        var currentFirstVisibleItem = firstVisibleItem
        var currentFirstVisibleItemOffset = firstVisibleItemOffset - deltaY
        var forwardIndex = firstVisibleItem
        var mainAxisUsed = -currentFirstVisibleItemOffset
        while (0 < currentFirstVisibleItem && minOffset > currentFirstVisibleItemOffset) {
            val view = viewFactory(currentFirstVisibleItem - 1)
            listView.children.add(0, view)
            currentFirstVisibleItem--
            currentFirstVisibleItemOffset += view.height
        }
        if (0 == currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
            currentFirstVisibleItemOffset = 0
            mainAxisUsed = 0
        }
        var invisibles: MutableList<View>? = null
        while (forwardIndex < listView.itemCount && mainAxisUsed < height) {
            val view = viewFactory(forwardIndex)
            mainAxisUsed += view.height
            forwardIndex++
            if (mainAxisUsed < minOffset) {
                currentFirstVisibleItem = forwardIndex
                currentFirstVisibleItemOffset -= view.height
                if (null == invisibles) {
                    invisibles = mutableListOf()
                }
                invisibles.add(view)
            } else {
                listView.children.add(view)
            }
        }
        if (mainAxisUsed < height) {
            //Move back.
            var delta = height - mainAxisUsed
            currentFirstVisibleItemOffset -= delta
            mainAxisUsed += delta
            while (0 < currentFirstVisibleItem && 0 > currentFirstVisibleItemOffset) {
                val view = invisibles?.lastOrNull() ?: viewFactory(currentFirstVisibleItem - 1)
                listView.children.add(0, view)
                currentFirstVisibleItemOffset += view.height
                currentFirstVisibleItem--
            }
        }
        scrollState.firstVisibleItem = currentFirstVisibleItem
        scrollState.firstVisibleItemOffset = currentFirstVisibleItemOffset
    }

    /**
     * Simulate the list scrolls down.
     */
    @Test
    fun testListMoveForward() {
        val listView = buildList()
        layoutChildren(listView)
        Assert.assertEquals(listView.children.size, 5)
        val scrollState = ScrollState()
        scrollBy(listView, scrollState, -5)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 5)

        scrollBy(listView, scrollState, -105)
        Assert.assertEquals(scrollState.firstVisibleItem, 1)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 10)

        scrollBy(listView, scrollState, -205)
        Assert.assertEquals(scrollState.firstVisibleItem, 3)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 15)
    }

    /**
     * Simulate the list scrolls up.
     */
    @Test
    fun testListMoveBackward() {
        val listView = buildList()
        layoutChildren(listView)
        Assert.assertEquals(listView.children.size, 5)
        val scrollState = ScrollState()
        scrollBy(listView, scrollState, -1000)
        Assert.assertEquals(scrollState.firstVisibleItem, 9)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 100)

        scrollBy(listView, scrollState, 200)
        Assert.assertEquals(scrollState.firstVisibleItem, 8)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 0)
    }

    @Test
    fun testListMoveBackwardToTop() {
        val listView = buildList()
        layoutChildren(listView)
        Assert.assertEquals(listView.children.size, 5)
        val scrollState = ScrollState()
        scrollByAndCheckBoundary(listView, scrollState, -150)
        Assert.assertEquals(scrollState.firstVisibleItem, 1)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 50)

        scrollByAndCheckBoundary(listView, scrollState, 200)
        Assert.assertEquals(scrollState.firstVisibleItem, 0)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 0)
    }

    /**
     * Simulate the list scrolls down to the bottom.
     */
    @Test
    fun testListMoveForwardToBottom() {
        val listView = buildList()
        layoutChildren(listView)
        Assert.assertEquals(listView.children.size, 5)
        val scrollState = ScrollState()
        scrollByAndCheckBoundary(listView, scrollState, -1450)
        Assert.assertEquals(scrollState.firstVisibleItem, 14)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 50)

        scrollByAndCheckBoundary(listView, scrollState, -300)
        Assert.assertEquals(scrollState.firstVisibleItem, 15)
        Assert.assertEquals(scrollState.firstVisibleItemOffset, 0)
    }

}