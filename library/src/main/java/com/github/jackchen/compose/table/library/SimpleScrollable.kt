package com.github.jackchen.compose.table.library

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [SimpleScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [SimpleScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [SimpleScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionState [InteractionState] that will be updated when this draggable is
 * being dragged, using [Interaction.Dragged].
 */
fun Modifier.scrollable(
    state: SimpleScrollableState,
    orientation: Orientation = Orientation.BOTH,
    enabled: Boolean = true,
    reverseDirection: Boolean = true,
    flingBehavior: SimpleFlingBehavior? = null,
    interactionState: SimpleInteractionState? = null
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollable"
        properties["state"] = state
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionState"] = interactionState
    },
    factory = {
        touchScrollImplementation(
            orientation,
            interactionState,
            reverseDirection,
            state,
            flingBehavior,
            enabled
        )
    }
)

/**
 * Contains the default values used by [scrollable]
 */
object ScrollableDefaults {

    /**
     * Create and remember default [SimpleFlingBehavior] that will represent natural fling curve.
     */
    @Composable
    fun flingBehavior(): SimpleFlingBehavior {
        val flingSpec = rememberSplineBasedDecay<Offset>()
        return remember(flingSpec) {
            DefaultFlingBehavior(flingSpec)
        }
    }
}


/**
 * Interface to specify fling behavior.
 *
 * When drag has ended with velocity in [scrollable], [performFling] is invoked to perform fling
 * animation and update state via [ScrollScope.scrollBy]
 */
@Stable
interface SimpleFlingBehavior {
    /**
     * Perform settling via fling animation with given velocity and suspend until fling has
     * finished.
     *
     * This functions is called with [ScrollScope] to drive the state change of the
     * [androidx.compose.foundation.gestures.ScrollableState] via [ScrollScope.scrollBy].
     *
     * This function must return correct velocity left after it is finished flinging in order to
     * guarantee proper nested scroll support.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     *
     * @return remaining velocity after fling operation has ended
     */
    suspend fun SimpleScrollScope.performFling(initialVelocity: Offset): Offset
}

@Composable
private fun Modifier.touchScrollImplementation(
    orientation: Orientation,
    interactionState: SimpleInteractionState?,
    reverseDirection: Boolean,
    controller: SimpleScrollableState,
    flingBehavior: SimpleFlingBehavior?,
    enabled: Boolean
): Modifier {
    DisposableEffect(interactionState) {
        onDispose {
            interactionState?.removeInteraction(SimpleInteraction.Dragged)
        }
    }

    val nestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }
    val scrollLogic = rememberUpdatedState(
        ScrollingLogic(
            orientation,
            reverseDirection,
            nestedScrollDispatcher,
            controller,
            flingBehavior ?: ScrollableDefaults.flingBehavior()
        )
    )
    val nestedScrollConnection = remember { scrollableNestedScrollConnection(scrollLogic) }
    val enabledState = rememberUpdatedState(enabled)
    val controllerState = rememberUpdatedState(controller)
    val interactionStateState = rememberUpdatedState(interactionState)
    val orientationState = rememberUpdatedState(orientation)
    return dragForEachGesture(
        orientation = orientationState,
        enabled = enabledState,
        scrollableState = controllerState,
        nestedScrollDispatcher = nestedScrollDispatcher,
        interactionState = interactionStateState,
        scrollLogic = scrollLogic
    ).nestedScroll(nestedScrollConnection, nestedScrollDispatcher.value)
}

@Composable
private fun Modifier.dragForEachGesture(
    orientation: State<Orientation>,
    enabled: State<Boolean>,
    scrollableState: State<SimpleScrollableState>,
    nestedScrollDispatcher: State<NestedScrollDispatcher>,
    interactionState: State<SimpleInteractionState?>,
    scrollLogic: State<ScrollingLogic>
): Modifier {
    fun Offset.axisValue(): Offset {
        return when (orientation.value) {
            Orientation.Vertical -> copy(x = 0f)
            Orientation.Horizontal -> copy(y = 0f)
            else -> this
        }
    }

    suspend fun PointerInputScope.initialDown(): Pair<PointerInputChange?, Offset> {
        var initialDelta = Offset.Zero
        return awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (!enabled.value) {
                null to initialDelta
            } else if (scrollableState.value.isScrollInProgress) {
                // since we start immediately we don't wait for slop and set initial delta to 0
                initialDelta = Offset.Zero
                down to initialDelta
            } else {
                val onSlopPassed = { event: PointerInputChange, overSlop: Offset ->
                    event.consumePositionChange()
                    initialDelta = overSlop
                }
                val result = awaitTouchSlopOrCancellation(down.id, onSlopPassed)
                (if (enabled.value) result else null) to initialDelta
            }
        }
    }

    suspend fun PointerInputScope.mainDragCycle(
        drag: PointerInputChange,
        initialDelta: Offset,
        velocityTracker: VelocityTracker,
    ): Boolean {
        var result = false
        try {
            scrollableState.value.scroll(MutatePriority.UserInput) {
                awaitPointerEventScope {
                    if (enabled.value) {
                        with(scrollLogic.value) {
                            dispatchScroll(initialDelta, NestedScrollSource.Drag)
                        }
                    }
                    velocityTracker.addPosition(drag.uptimeMillis, drag.position)
                    val dragTick = { event: PointerInputChange ->
                        velocityTracker.addPosition(event.uptimeMillis, event.position)
                        val delta = event.positionChange().axisValue()
                        if (enabled.value) {
                            with(scrollLogic.value) {
                                dispatchScroll(delta, NestedScrollSource.Drag)
                            }
                        }
                        event.consumePositionChange()
                    }
                    result = horizontalDrag(drag.id, dragTick)
                            || verticalDrag(drag.id, dragTick)
                }
            }
        } catch (c: CancellationException) {
            result = false
        }
        return result
    }

    suspend fun fling(velocity: Velocity) {
        val preConsumedByParent = nestedScrollDispatcher.value.dispatchPreFling(velocity)
        val available = velocity - preConsumedByParent
        val velocityLeft = scrollLogic.value.doFlingAnimation(available)
        nestedScrollDispatcher.value.dispatchPostFling(available - velocityLeft, velocityLeft)
    }

    val scrollLambda: suspend PointerInputScope.() -> Unit = remember {
        {
            forEachGesture {
                val (startEvent, initialDelta) = initialDown()
                if (startEvent != null) {
                    val velocityTracker = VelocityTracker()
                    // remember enabled state when we add interaction to remove later if needed
                    val enabledWhenInteractionAdded = enabled.value
                    if (enabledWhenInteractionAdded) {
                        interactionState.value?.addInteraction(SimpleInteraction.Dragged)
                    }
                    val isDragSuccessful = mainDragCycle(startEvent, initialDelta, velocityTracker)
                    if (enabledWhenInteractionAdded) {
                        interactionState.value?.removeInteraction(SimpleInteraction.Dragged)
                    }
                    if (isDragSuccessful) {
                        nestedScrollDispatcher.value.coroutineScope.launch {
                            fling(velocityTracker.calculateVelocity())
                        }
                    }
                }
            }
        }
    }
    return pointerInput(scrollLambda, scrollLambda)
}

private class ScrollingLogic(
    val orientation: Orientation,
    val reverseDirection: Boolean,
    val nestedScrollDispatcher: State<NestedScrollDispatcher>,
    val scrollableState: SimpleScrollableState,
    val flingBehavior: SimpleFlingBehavior
) {

    fun Offset.reverseIfNeeded(): Offset =
        if (reverseDirection) -this else this

    fun Offset.toVelocity(): Velocity {
        return when (orientation) {
            Orientation.Vertical -> Velocity(x = 0f, y = y)
            Orientation.Horizontal -> Velocity(x = x, y = 0f)
            else -> Velocity(x, y)
        }
    }

    fun Velocity.toOffset(): Offset {
        return when (orientation) {
            Orientation.Vertical -> Offset(x = 0f, y = y)
            Orientation.Horizontal -> Offset(x = x, y = 0f)
            else -> Offset(x, y)
        }
    }

    fun SimpleScrollScope.dispatchScroll(scrollDelta: Offset, source: NestedScrollSource): Offset {
        val preConsumedByParent = nestedScrollDispatcher.value
            .dispatchPreScroll(scrollDelta, source)

        val scrollAvailable = scrollDelta - preConsumedByParent
        val consumed = scrollBy(scrollAvailable.reverseIfNeeded())
            .reverseIfNeeded()
        val leftForParent = scrollAvailable - consumed
        nestedScrollDispatcher.value.dispatchPostScroll(consumed, leftForParent, source)
        return leftForParent
    }

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.reverseIfNeeded())
                .reverseIfNeeded()
        }
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        if (abs(available.x) > 1f || abs(available.y) > 1f) scrollableState.scroll {
            val outerScopeScroll: (Offset) -> Offset =
                { delta -> this.dispatchScroll(delta, NestedScrollSource.Fling) }
            val scope = object : SimpleScrollScope {
                override fun scrollBy(pixels: Offset): Offset {
                    return outerScopeScroll.invoke(pixels)
                }
            }
            with(scope) {
                with(flingBehavior) {
                    result = performFling(available.toOffset()).toVelocity()
                }
            }
        }
        return result
    }
}

private fun scrollableNestedScrollConnection(
    scrollLogic: State<ScrollingLogic>
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = scrollLogic.value.performRawScroll(available)

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        val velocityLeft = scrollLogic.value.doFlingAnimation(available)
        return available - velocityLeft
    }
}

private class DefaultFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Offset>
) : SimpleFlingBehavior {

    override suspend fun SimpleScrollScope.performFling(initialVelocity: Offset): Offset {
        var velocityLeft = initialVelocity
        var lastValue = Offset.Zero
        offsetAnimationState(
            initialValue = Offset.Zero,
            initialVelocity = initialVelocity,
        ).animateDecay(flingDecay) {
            val delta = value - lastValue
            val left = scrollBy(delta)
            lastValue = value
            velocityLeft = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(left.x) > 0.5f && abs(left.y) > 0.5f) this.cancelAnimation()
        }
        return velocityLeft
    }

    private fun offsetAnimationState(
        initialValue: Offset,
        initialVelocity: Offset = Offset.Zero,
        lastFrameTimeNanos: Long = AnimationConstants.UnspecifiedTime,
        finishedTimeNanos: Long = AnimationConstants.UnspecifiedTime,
        isRunning: Boolean = false
    ): AnimationState<Offset, AnimationVector2D> {
        return AnimationState(
            Offset.VectorConverter,
            initialValue,
            AnimationVector(initialVelocity.x, initialVelocity.y),
            lastFrameTimeNanos,
            finishedTimeNanos,
            isRunning
        )
    }
}
