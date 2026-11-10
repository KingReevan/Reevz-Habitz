package com.reevan.reevzhabitz.ui.habitorder

/*
 * Habit Order's rules, pure so they can be tested without a screen: moving a habit, and the
 * steps a dragged card takes as it passes its neighbours.
 */

/** This list with the item at [from] moved to [to], the ones in between shifting up or down. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * The order to show: the user's unsaved [edited] order if they have moved something, else the
 * [stored] one. Habits gone meanwhile drop out; any that appear go on top, as a new habit does.
 */
fun shownOrder(edited: List<Long>?, stored: List<Long>): List<Long> {
    if (edited == null) return stored
    val present = stored.toSet()
    val kept = edited.filter { it in present }
    val known = kept.toSet()
    return stored.filter { it !in known } + kept
}

/** Where a drag has got to: the order now, and the dragged card's offset from its own slot. */
data class DragStep(val order: List<Long>, val offset: Float)

/**
 * Moves the dragged [id] one slot at a time while it sits more than half a [slot] (a card plus
 * its divider, in pixels) away from its own place, so it swaps with a neighbour as soon as it
 * covers most of it. The [offset] comes back reduced by each slot moved, keeping the card under
 * the finger. Stops at either end.
 */
fun dragStep(order: List<Long>, id: Long, offset: Float, slot: Float): DragStep {
    var index = order.indexOf(id)
    if (index < 0 || slot <= 0f) return DragStep(order, offset)
    var list = order
    var left = offset
    while (left > slot / 2 && index < list.lastIndex) {
        list = list.moved(index, index + 1)
        index++
        left -= slot
    }
    while (left < -slot / 2 && index > 0) {
        list = list.moved(index, index - 1)
        index--
        left += slot
    }
    return DragStep(list, left)
}
