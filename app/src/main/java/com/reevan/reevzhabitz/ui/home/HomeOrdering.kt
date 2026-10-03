package com.reevan.reevzhabitz.ui.home

import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.common.habitComparator

/** Home's order: habits still to do on top, done ones sunk to the bottom, each group in [sort]. */
fun orderForHome(items: List<HabitOnDay>, sort: HomeSort): List<HabitOnDay> {
    val bySort = sort.habitComparator()
    return items.sortedWith(
        compareBy<HabitOnDay> { it.done }.thenComparing({ it.habit }, bySort),
    )
}
