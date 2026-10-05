package com.reevan.reevzhabitz.ui.home

import com.reevan.reevzhabitz.data.HabitOnDay

/** True when there are habits today and every one is ticked. No habits is not a finished day. */
fun isDayComplete(habits: List<HabitOnDay>): Boolean = habits.isNotEmpty() && habits.all { it.done }

/**
 * Spots the moment the user finishes the day, to celebrate it: the list in which a habit they
 * just ticked shows as done and every habit is done.
 *
 * It can't judge at tap time. A tick reaches the database a moment after the tap, and two quick
 * taps can land in the same list, so it remembers which ticks are still on their way. A day that
 * ends up complete any other way — the last unticked habit removed from the Menu — doesn't count.
 */
class DayCompletionWatcher {

    /** Habits the user ticked that the list doesn't show as done yet. */
    private val landing = mutableSetOf<Long>()

    fun ticked(habitId: Long) {
        landing += habitId
    }

    /** Call with every new list of today's habits. True for the one where a tick finished the day. */
    fun completedByTick(habits: List<HabitOnDay>): Boolean {
        val landed = habits.any { it.done && it.habit.id in landing }
        // Keep only ticks still on their way: drop those now done and habits no longer listed.
        landing.retainAll(habits.filterNot { it.done }.map { it.habit.id }.toSet())
        return landed && isDayComplete(habits)
    }
}
