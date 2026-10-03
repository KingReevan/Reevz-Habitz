package com.reevan.reevzhabitz.ui.common

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HomeSort

/**
 * The order a [HomeSort] puts habits in. Shared by every list that follows Home's sort (Home,
 * Remove Habit), so the same habit sits in the same place on each.
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
