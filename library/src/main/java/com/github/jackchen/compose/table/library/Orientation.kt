package com.github.jackchen.compose.table.library
/**
 * Class to define possible directions in which common gesture modifiers like [draggable] and
 * [scrollable] can drag.
 */
enum class Orientation {
    /**
     * Vertical orientation representing Y axis
     */
    Vertical,

    /**
     * Horizontal orientation representing X axis.
     *
     * Note: this value specifies just the axis, not the direction (left-to-right or
     * right-to-left). To support RTL cases, use `reverseDirection = true` on [scrollable] and
     * [draggable].
     */
    Horizontal,

    /**
     * Both horizontal and vertical.
     */
    BOTH
}