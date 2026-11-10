package com.reevan.reevzhabitz.ui.home

import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.ui.common.HomePositionOrder

/**
 * Home's order: habits still to do on top, done ones sunk to the bottom, each group in the order
 * set in Habit Order — so the next habit to do is always the top one.
 */
fun orderForHome(items: List<HabitOnDay>): List<HabitOnDay> =
    items.sortedWith(compareBy<HabitOnDay> { it.done }.thenComparing({ it.habit }, HomePositionOrder))
