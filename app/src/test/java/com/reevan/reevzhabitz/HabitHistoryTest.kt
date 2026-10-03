package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.ui.statistics.DayStatus
import com.reevan.reevzhabitz.ui.statistics.HabitHistory
import com.reevan.reevzhabitz.ui.statistics.StatsSummary
import com.reevan.reevzhabitz.ui.statistics.monthGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class HabitHistoryTest {

    private val today = LocalDate.of(2026, 10, 15)
    private val start = LocalDate.of(2026, 10, 1)

    private fun d(day: Int, month: Int = 10) = LocalDate.of(2026, month, day)

    private fun history(
        done: List<LocalDate>,
        startDate: LocalDate = start,
        deletedOn: LocalDate? = null,
        createdOn: LocalDate = startDate.minusDays(1),
        now: LocalDate = today,
    ) = HabitHistory(startDate, createdOn, deletedOn, done, now)

    // ---- Calendar colours ----------------------------------------------------------------

    @Test
    fun status_followsTheSpecRules() {
        val h = history(done = listOf(d(10), d(15)))
        assertEquals(DayStatus.MISSED, h.statusOn(d(28, 9)))   // before start: red
        assertEquals(DayStatus.MISSED, h.statusOn(d(9)))       // due, not done: red
        assertEquals(DayStatus.DONE, h.statusOn(d(10)))        // done: green
        assertEquals(DayStatus.DONE, h.statusOn(d(15)))        // today, done: green
        assertEquals(DayStatus.FUTURE, h.statusOn(d(16)))      // future: grey
    }

    @Test
    fun status_todayUntickedIsRed() {
        assertEquals(DayStatus.MISSED, history(done = emptyList()).statusOn(today))
    }

    @Test
    fun status_deletedHabit_greyAfterDeletion_deletionDayGreyUnlessDone() {
        val deleted = d(12)
        val h = history(done = listOf(d(11)), deletedOn = deleted)
        assertEquals(DayStatus.DONE, h.statusOn(d(11)))
        assertEquals(DayStatus.AFTER_DELETION, h.statusOn(deleted))
        assertEquals(DayStatus.AFTER_DELETION, h.statusOn(d(13)))
        assertEquals(DayStatus.FUTURE, h.statusOn(d(20)))   // beyond today is still just future

        val tickedThenDeleted = history(done = listOf(d(12)), deletedOn = deleted)
        assertEquals(DayStatus.DONE, tickedThenDeleted.statusOn(deleted))
    }

    // ---- Numbers -------------------------------------------------------------------------

    @Test
    fun summary_countsOnlyFromTheStartDate() {
        // 1–14 Oct have settled: 14 due days. Today (15th) unticked doesn't count yet.
        val h = history(done = listOf(d(1), d(2), d(3), d(10)))
        assertEquals(4, h.summary.done)
        assertEquals(14, h.summary.due)
    }

    @Test
    fun summary_todayCountsOnceTicked() {
        val h = history(done = listOf(d(14), d(15)))
        assertEquals(StatsSummary(done = 2, due = 15, currentStreak = 2, longestStreak = 2), h.summary)
    }

    @Test
    fun currentStreak_survivesAnUntickedToday() {
        // Done 12th–14th; today (15th) not yet. The streak is still alive at 3.
        val h = history(done = listOf(d(12), d(13), d(14)))
        assertEquals(3, h.summary.currentStreak)
    }

    @Test
    fun currentStreak_breaksWhenYesterdayWasMissed() {
        // Done 10th–13th, missed the 14th: the streak is gone, though the run is still the longest.
        val h = history(done = listOf(d(10), d(11), d(12), d(13)))
        assertEquals(0, h.summary.currentStreak)
        assertEquals(4, h.summary.longestStreak)
    }

    @Test
    fun longestStreak_picksTheLongestRun() {
        val h = history(done = listOf(d(1), d(2), d(4), d(5), d(6), d(7), d(9), d(14), d(15)))
        assertEquals(4, h.summary.longestStreak)   // 4th–7th
        assertEquals(2, h.summary.currentStreak)   // 14th–15th
    }

    @Test
    fun perfectRecord() {
        val h = history(done = (1..15).map { d(it) })
        assertEquals(StatsSummary(done = 15, due = 15, currentStreak = 15, longestStreak = 15), h.summary)
    }

    @Test
    fun startsToday_untickedHasNothingToCountYet() {
        assertEquals(StatsSummary(0, 0, 0, 0), history(done = emptyList(), startDate = today).summary)
    }

    @Test
    fun startsToday_tickedIsOneForOne() {
        assertEquals(
            StatsSummary(done = 1, due = 1, currentStreak = 1, longestStreak = 1),
            history(done = listOf(today), startDate = today).summary,
        )
    }

    @Test
    fun notStartedYet_isAllZero() {
        assertEquals(StatsSummary(0, 0, 0, 0), history(done = emptyList(), startDate = d(20)).summary)
    }

    @Test
    fun deletedHabit_numbersFreezeAtDeletion() {
        // Due 1st–11th (11 days; the 12th is the deletion day, unticked → not counted).
        val h = history(done = listOf(d(9), d(10), d(11)), deletedOn = d(12))
        assertEquals(StatsSummary(done = 3, due = 11, currentStreak = 3, longestStreak = 3), h.summary)
    }

    @Test
    fun deletedHabit_deletionDayCountsIfTicked() {
        val h = history(done = listOf(d(11), d(12)), deletedOn = d(12))
        assertEquals(StatsSummary(done = 2, due = 12, currentStreak = 2, longestStreak = 2), h.summary)
    }

    @Test
    fun deletedBeforeItStarted_hasNoNumbers() {
        assertEquals(
            StatsSummary(0, 0, 0, 0),
            history(done = emptyList(), startDate = d(20), deletedOn = d(14)).summary,
        )
    }

    @Test
    fun streaksRunAcrossMonthAndYearEnds() {
        val h = HabitHistory(
            startDate = LocalDate.of(2026, 12, 30),
            createdOn = LocalDate.of(2026, 12, 29),
            deletedOn = null,
            doneDays = listOf(LocalDate.of(2026, 12, 30), LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1)),
            today = LocalDate.of(2027, 1, 2),
        )
        assertEquals(StatsSummary(done = 3, due = 3, currentStreak = 3, longestStreak = 3), h.summary)
    }

    @Test
    fun leapDay_isJustAnotherDay() {
        val h = HabitHistory(
            startDate = LocalDate.of(2028, 2, 28),
            createdOn = LocalDate.of(2028, 2, 28),
            deletedOn = null,
            doneDays = listOf(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 1)),
            today = LocalDate.of(2028, 3, 2),
        )
        assertEquals(StatsSummary(done = 3, due = 3, currentStreak = 3, longestStreak = 3), h.summary)
    }

    // ---- Month paging --------------------------------------------------------------------

    @Test
    fun months_activeHabitPagesFromCreationToNowAndOpensOnNow() {
        val h = history(done = emptyList(), startDate = d(1), createdOn = d(28, 9))
        assertEquals(YearMonth.of(2026, 9), h.firstMonth)
        assertEquals(YearMonth.of(2026, 10), h.lastMonth)
        assertEquals(YearMonth.of(2026, 10), h.openingMonth)
    }

    @Test
    fun months_deletedHabitOpensOnTheDeletionMonth() {
        val h = HabitHistory(
            startDate = LocalDate.of(2026, 6, 1),
            createdOn = LocalDate.of(2026, 5, 30),
            deletedOn = LocalDate.of(2026, 8, 20),
            doneDays = emptyList(),
            today = today,
        )
        assertEquals(YearMonth.of(2026, 5), h.firstMonth)
        assertEquals(YearMonth.of(2026, 8), h.lastMonth)
        assertEquals(YearMonth.of(2026, 8), h.openingMonth)
    }

    // ---- Month grid ----------------------------------------------------------------------

    @Test
    fun monthGrid_october2026_startingSunday() {
        // 1 Oct 2026 is a Thursday: four blanks (Sun–Wed) first.
        val cells = monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(35, cells.size)
        (0..3).forEach { assertNull(cells[it]) }
        assertEquals(d(1), cells[4])
        assertEquals(d(31), cells[34])
    }

    @Test
    fun monthGrid_october2026_startingMonday() {
        val cells = monthGrid(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(35, cells.size)
        (0..2).forEach { assertNull(cells[it]) }
        assertEquals(d(1), cells[3])
        assertNull(cells[34])   // 31st is a Saturday; Sunday's cell is blank
    }

    @Test
    fun monthGrid_needsSixRowsSometimes_andIsAlwaysWholeWeeks() {
        // August 2026 starts on a Saturday: with Sunday first it spans six rows.
        assertEquals(42, monthGrid(YearMonth.of(2026, 8), DayOfWeek.SUNDAY).size)
        // February 2026 starts on a Sunday and has 28 days: exactly four rows.
        assertEquals(28, monthGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY).size)
        (1..12).forEach { m ->
            DayOfWeek.entries.forEach { first ->
                val cells = monthGrid(YearMonth.of(2026, m), first)
                assertEquals(0, cells.size % 7)
                assertEquals(YearMonth.of(2026, m).lengthOfMonth(), cells.count { it != null })
                assertEquals(first, cells.first { it != null }!!.let { day ->
                    day.minusDays(cells.indexOf(day).toLong()).dayOfWeek
                })
            }
        }
    }
}
