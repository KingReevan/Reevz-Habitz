package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.ui.habitform.HabitDraft
import com.reevan.reevzhabitz.ui.habitform.defaultStartDate
import com.reevan.reevzhabitz.ui.habitform.isSelectableStartDate
import com.reevan.reevzhabitz.ui.habitform.startDateFor
import com.reevan.reevzhabitz.util.datePickerMillisToLocalDate
import com.reevan.reevzhabitz.util.formatLongDate
import com.reevan.reevzhabitz.util.toDatePickerMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone

class HabitDraftTest {

    private val today = LocalDate.of(2026, 10, 3)

    private val complete = HabitDraft(
        name = "  Drink Water ",
        description = " Eight glasses. ",
        colorKey = "sky",
        iconKey = "water",
        startDate = today.plusDays(1),
    )

    @Test
    fun complete_needsEveryField() {
        assertTrue(complete.isComplete)
        assertFalse(complete.copy(description = "").isComplete)
        assertFalse(complete.copy(description = "  ").isComplete)
        assertFalse(complete.copy(name = "   ").isComplete)
        assertFalse(complete.copy(colorKey = null).isComplete)
        assertFalse(complete.copy(iconKey = null).isComplete)
    }

    @Test
    fun toHabit_trimsTextAndKeepsTheChoices() {
        val habit = complete.toHabit(today, createdAt = 1234L)
        assertEquals("Drink Water", habit.name)
        assertEquals("Eight glasses.", habit.description)
        assertEquals("sky", habit.colorKey)
        assertEquals("water", habit.iconKey)
        assertEquals(today.plusDays(1), habit.startDate)
        assertEquals(1234L, habit.createdAt)
        assertEquals(0L, habit.id)
        assertNull(habit.deletedOn)
    }

    @Test
    fun toHabit_capitalisesTheNameAsTheFieldDisplaysIt() {
        // The name field capitalises on screen only, so raw typing reaches the draft lowercase.
        val habit = complete.copy(name = " go for a 5km run ").toHabit(today, 0)
        assertEquals("Go For A 5km Run", habit.name)
    }

    @Test
    fun toHabit_startDateLeftInThePastMovesUpToToday() {
        // Form opened yesterday with "today" picked, Create pressed after midnight.
        val habit = complete.copy(startDate = today.minusDays(1)).toHabit(today, 0)
        assertEquals(today, habit.startDate)
    }

    @Test
    fun toHabit_refusesAnIncompleteDraft() {
        assertThrows(IllegalArgumentException::class.java) {
            complete.copy(iconKey = null).toHabit(today, 0)
        }
    }

    @Test
    fun startDate_defaultsToTomorrow_andAllowsTodayButNotThePast() {
        assertEquals(LocalDate.of(2026, 10, 4), defaultStartDate(today))
        assertTrue(isSelectableStartDate(today, today))
        assertTrue(isSelectableStartDate(today.plusYears(1), today))
        assertFalse(isSelectableStartDate(today.minusDays(1), today))
    }

    @Test
    fun startDate_untouchedFollowsTomorrowAcrossMidnight_pickedStaysPut() {
        // Form opened on the 3rd; the user leaves Start From alone and creates after midnight.
        assertEquals(LocalDate.of(2026, 10, 4), startDateFor(null, today))
        assertEquals(LocalDate.of(2026, 10, 5), startDateFor(null, today.plusDays(1)))
        // A date the user picked is theirs, whatever the clock does.
        val picked = LocalDate.of(2026, 10, 9).toEpochDay()
        assertEquals(LocalDate.of(2026, 10, 9), startDateFor(picked, today.plusDays(1)))
    }

    @Test
    fun startDate_defaultCrossesMonthAndYearEnds() {
        assertEquals(LocalDate.of(2026, 11, 1), defaultStartDate(LocalDate.of(2026, 10, 31)))
        assertEquals(LocalDate.of(2027, 1, 1), defaultStartDate(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun datePickerMillis_roundTripTheSameDayInAnyPhoneZone() {
        val original = TimeZone.getDefault()
        try {
            listOf("Asia/Kolkata", "America/Los_Angeles", "Pacific/Kiritimati", "Pacific/Pago_Pago")
                .forEach { zone ->
                    TimeZone.setDefault(TimeZone.getTimeZone(zone))
                    listOf(today, LocalDate.of(2028, 2, 29), LocalDate.of(2026, 12, 31)).forEach {
                        assertEquals("$it in $zone", it, datePickerMillisToLocalDate(it.toDatePickerMillis()))
                    }
                }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun longDate_spellsOutTheDay() {
        assertEquals("Sunday, 04 October 2026", formatLongDate(today.plusDays(1), Locale.ENGLISH))
    }
}
