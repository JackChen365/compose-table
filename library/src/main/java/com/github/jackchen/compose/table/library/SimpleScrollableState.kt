package com.github.jackchen.compose.table.library;

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope

/**
 * An object representing something that can be scrolled. This interface is implemented by states
 * of scrollable containers such as [androidx.compose.foundation.lazy.LazyListState] or
 * [androidx.compose.foundation.ScrollState] in order to provide low-level scrolling control via
 * [scroll], as well as allowing for higher-level scrolling functions like
 * [animateScrollBy] to be implemented as extension
 * functions on [SimpleScrollableState].
 *
 * Subclasses may also have their own methods that are specific to their interaction paradigm, such
 * as [androidx.compose.foundation.lazy.LazyListState.scrollToItem].
 *
 * @see androidx.compose.foundation.gestures.animateScrollBy
 * @see androidx.compose.foundation.gestures.scrollable
 */
interface SimpleScrollableState {
    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [SimpleScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this
     * object) in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere with the [scrollPriority] higher or equal to ongoing
     * scroll, ongoing scroll will be canceled.
     */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend SimpleScrollScope.() -> Unit
    )

    /**
     * Dispatch scroll delta in pixels avoiding all scroll related mechanisms.
     *
     * **NOTE:** unlike [scroll], dispatching any delta with this method won't trigger nested
     * scroll, won't stop ongoing scroll/drag animation and will bypass scrolling of any priority.
     * This method will also ignore `reverseDirection` and other parameters set in scrollable.
     *
     * This method is used internally for nested scrolling dispatch and other low level
     * operations, allowing implementers of [SimpleScrollableState] influence the consumption as suits
     * them. Manually dispatching delta via this method will likely result in a bad user experience,
     * you must prefer [scroll] method over this one.
     *
     * @param delta amount of scroll dispatched in the nested scroll process
     *
     * @return the amount of delta consumed
     */
    fun dispatchRawDelta(delta: Offset): Offset

    /**
     * Whether this [SimpleScrollableState] is currently scrolling by gesture, fling or programmatically or
     * not.
     */
    val isScrollInProgress: Boolean
}

/**
 * Default implementation of [SimpleScrollableState] interface that contains necessary information about the
 * ongoing fling and provides smooth scrolling capabilities.
 *
 * This is the simplest way to set up a [scrollable] modifier. When constructing this
 * [SimpleScrollableState], you must provide a [consumeScrollDelta] lambda, which will be invoked whenever
 * scroll happens (by gesture input, by smooth scrolling, by flinging or nested scroll) with the
 * delta in pixels. The amount of scrolling delta consumed must be returned from this lambda to
 * ensure proper nested scrolling behaviour.
 *
 * @param consumeScrollDelta callback invoked when drag/fling/smooth scrolling occurs. The
 * callback receives the delta in pixels. Callers should update their state in this lambda and
 * return the amount of delta consumed
 */
fun SimpleScrollableState(consumeScrollDelta: (Offset) -> Offset): SimpleScrollableState {
    return DefaultScrollableState(consumeScrollDelta)
}

/**
 * Scope used for suspending scroll blocks
 */
interface SimpleScrollScope {
    /**
     * Attempts to scroll forward by [pixels] px.
     *
     * @return the amount of the requested scroll that was consumed (that is, how far it scrolled)
     */
    fun scrollBy(pixels: Offset): Offset
}

private class DefaultScrollableState(val onDelta: (Offset) -> Offset) : SimpleScrollableState {

    private val scrollScope: SimpleScrollScope = object : SimpleScrollScope {
        override fun scrollBy(pixels: Offset): Offset = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    private val isScrollingState = mutableStateOf(false)

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend SimpleScrollScope.() -> Unit
    ): Unit = coroutineScope {
        scrollMutex.mutateWith(scrollScope, scrollPriority) {
            isScrollingState.value = true
            block()
            isScrollingState.value = false
        }
    }

    override fun dispatchRawDelta(delta: Offset): Offset {
        return onDelta(delta)
    }

    override val isScrollInProgress: Boolean
        get() = isScrollingState.value
}
