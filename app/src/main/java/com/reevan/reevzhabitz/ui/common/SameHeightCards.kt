package com.reevan.reevzhabitz.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints

/** Tags the children of [SameHeightCards] that are cards. */
private const val CARD = "card"

/** Marks a [HabitCard] inside [SameHeightCards] as one of the cards to make the same height. */
fun Modifier.sameHeightCard(): Modifier = layoutId(CARD)

/**
 * Stacks its children top to bottom, like a Column, but makes every child marked
 * [sameHeightCard] as tall as the tallest of them, so a card whose description wraps to two lines
 * doesn't stand out from one-line cards. Headings and dividers keep their own heights. Used by
 * the Statistics, Edit Habit and Remove Habit lists.
 *
 * Not lazy: the tallest card has to be known before any is placed, and the lists are short (one
 * card per habit). Scroll it with `Modifier.verticalScroll`.
 */
@Composable
fun SameHeightCards(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val each = Constraints(minWidth = width, maxWidth = width)
        val tallest = measurables.filter { it.layoutId == CARD }
            .maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
        val placeables = measurables.map {
            it.measure(if (it.layoutId == CARD) each.copy(minHeight = tallest, maxHeight = tallest) else each)
        }
        // At least the screen's height when scrolled, so a short list still starts at the top.
        val height = maxOf(placeables.sumOf { it.height }, constraints.minHeight)
        layout(width, height) {
            var y = 0
            placeables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}
