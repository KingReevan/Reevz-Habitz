package com.reevan.reevzhabitz.data

import java.time.DayOfWeek

/**
 * The days of the week a habit is due on ([Habit.days]), stored in `habits.days` as a 7-bit mask:
 * Monday is bit 0 (1) … Sunday bit 6 (64). 127 — every day — is the default, and what every habit
 * made before schedules existed got from the v1 → v2 migration.
 */
object Weekdays {
    /** Every day of the week: the default schedule. */
    val EveryDay: Set<DayOfWeek> = DayOfWeek.entries.toSet()

    /** [EveryDay] as a mask. The column's SQL default, `"127"` in [Habit], must match it. */
    const val EVERY_DAY_MASK = 127

    /** The mask bit for [day]. */
    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun toMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or bit(day) }

    /** The days in [mask]; bits beyond Sunday are ignored. */
    fun fromMask(mask: Int): Set<DayOfWeek> = DayOfWeek.entries.filter { mask and bit(it) != 0 }.toSet()
}
