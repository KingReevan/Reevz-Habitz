package com.reevan.reevzhabitz.ui.common

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HomeSort

/**
 * Home's order, the one set in Habit Order: by [Habit.homePosition], lowest first. Ties — habits
 * stored without a place of their own — put the newest first, as a new habit goes on top, then
 * the higher id, so the order never shuffles between recompositions.
 */
val HomePositionOrder: Comparator<Habit> =
    compareBy<Habit> { it.homePosition }
        .thenByDescending { it.createdAt }
        .thenByDescending { it.id }

/**
 * The order a [HomeSort] puts habits in: the Edit Habit, Remove Habit and Statistics lists, so
 * the same habit sits in the same place on each. (Home follows [HomePositionOrder] instead.)
 *
 * Ties — the same name, or created in the same millisecond — fall back to the id, so the order
 * never shuffles between recompositions.
 */
fun HomeSort.habitComparator(): Comparator<Habit> {
    val primary: Comparator<Habit> = when (this) {
        HomeSort.ALPHABETICAL -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        HomeSort.NEWEST_FIRST -> compareByDescending { it.createdAt }
        HomeSort.OLDEST_FIRST -> compareBy { it.createdAt }
    }
    return primary.thenBy { it.id }
}

fun List<Habit>.sortedFor(sort: HomeSort): List<Habit> = sortedWith(sort.habitComparator())
