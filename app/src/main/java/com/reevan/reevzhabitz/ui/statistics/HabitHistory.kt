package com.reevan.reevzhabitz.ui.statistics

import java.time.LocalDate
import java.time.YearMonth

/** How a day's circle is drawn in a habit's calendar. */
enum class DayStatus {
    /** Ticked that day. Green. */
    DONE,

    /** Not ticked — including days before the habit started, and today until it is ticked. Red. */
    MISSED,

    /** Still to come. Grey. */
    FUTURE,

    /** After the habit was deleted: it no longer existed, so neither done nor missed. Grey. */
    AFTER_DELETION,
}

/** The strip's three numbers. [done] out of [due] reads as e.g. "50/65". */
data class StatsSummary(
    val done: Int,
    val due: Int,
    val currentStreak: Int,
    val longestStreak: Int,
)

/**
 * One habit's history and every statistic derived from it. Pure — no Android, no database — so the
 * rules can be tested exhaustively.
 *
 * The rules (agreed with the owner):
 * - A habit is due every day from [startDate]. Days before it show red in the calendar but never
 *   count in the numbers.
 * - A day "settles" once it is over. Today — or for a deleted habit, its deletion day — has not
 *   settled: it counts in the numbers only if it was ticked, so an unticked today neither lowers
 *   X/Y nor breaks the streak until it ends (the Duolingo / Snapchat rule). The calendar still
 *   shows an unticked today as red.
 * - For a deleted habit, everything after [deletedOn] is grey and counts for nothing; the deletion
 *   day itself counts only if ticked. Its numbers are frozen as they stood when it was deleted.
 */
class HabitHistory(
    val startDate: LocalDate,
    /** The calendar day the habit was created; with [startDate], bounds how far back it pages. */
    val createdOn: LocalDate,
    val deletedOn: LocalDate?,
    doneDays: Collection<LocalDate>,
    val today: LocalDate,
) {
    private val done: Set<LocalDate> = doneDays.toHashSet()

    /** The open day: today, or the deletion day for a deleted habit. */
    private val openDay: LocalDate = deletedOn ?: today

    /** The last day that is over and counts whether or not it was ticked. */
    private val lastSettledDay: LocalDate = openDay.minusDays(1)

    fun statusOn(day: LocalDate): DayStatus = when {
        day.isAfter(today) -> DayStatus.FUTURE
        deletedOn != null && day.isAfter(deletedOn) -> DayStatus.AFTER_DELETION
        day in done -> DayStatus.DONE
        day == deletedOn -> DayStatus.AFTER_DELETION
        else -> DayStatus.MISSED
    }

    val summary: StatsSummary by lazy { computeSummary() }

    private fun computeSummary(): StatsSummary {
        val openDayCounts = !openDay.isBefore(startDate) && openDay in done
        // The last day that counts towards the numbers.
        val lastCounted = if (openDayCounts) openDay else lastSettledDay
        if (lastCounted.isBefore(startDate)) return StatsSummary(0, 0, 0, 0)

        var due = 0
        var doneCount = 0
        var run = 0
        var longest = 0
        var day = startDate
        while (!day.isAfter(lastCounted)) {
            due++
            if (day in done) {
                doneCount++
                run++
                if (run > longest) longest = run
            } else {
                run = 0
            }
            day = day.plusDays(1)
        }
        // The loop ends on lastCounted, so the run still open there is the current streak.
        return StatsSummary(done = doneCount, due = due, currentStreak = run, longestStreak = longest)
    }

    /** The earliest month the calendar pages back to: whichever came first, creation or start. */
    val firstMonth: YearMonth = YearMonth.from(minOf(createdOn, startDate))

    /** The latest month it pages forward to: now, or the deletion month for a deleted habit. */
    val lastMonth: YearMonth = YearMonth.from(openDay).let { if (it < firstMonth) firstMonth else it }

    /** The month the calendar opens on — the current one, or the deletion month. */
    val openingMonth: YearMonth get() = lastMonth
}

/**
 * The cells of [month]'s calendar, row by row: nulls for the blanks before the 1st and after the
 * last day, so the total is always a whole number of weeks.
 */
fun monthGrid(month: YearMonth, firstDayOfWeek: java.time.DayOfWeek): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells = ArrayList<LocalDate?>(42)
    repeat(leading) { cells.add(null) }
    for (d in 1..month.lengthOfMonth()) cells.add(month.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}
