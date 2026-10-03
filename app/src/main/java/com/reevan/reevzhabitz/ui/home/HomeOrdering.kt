package com.reevan.reevzhabitz.ui.home

import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort

/**
 * Home's order: habits still to do on top, done ones sunk to the bottom, each group in [sort].
 *
 * Ties — two habits with the same name, or created in the same millisecond — fall back to the id,
 * so the order never shuffles between recompositions.
 */
fun orderForHome(items: List<HabitOnDay>, sort: HomeSort): List<HabitOnDay> {
    val within: Comparator<HabitOnDay> = when (sort) {
        HomeSort.ALPHABETICAL ->
            compareBy(String.CASE_INSENSITIVE_ORDER) { it: HabitOnDay -> it.habit.name }
        HomeSort.NEWEST_FIRST -> compareByDescending { it.habit.createdAt }
        HomeSort.OLDEST_FIRST -> compareBy { it.habit.createdAt }
    }
    return items.sortedWith(
        compareBy<HabitOnDay> { it.done }
            .then(within)
            .thenBy { it.habit.id },
    )
}
