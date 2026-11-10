package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.data.Converters
import com.reevan.reevzhabitz.data.Weekdays
import com.reevan.reevzhabitz.util.formatWeekdays
import com.reevan.reevzhabitz.util.weekStartingOn
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.util.Locale

class WeekdaysTest {

    @Test
    fun mask_mondayIsTheLowBit_everyDayIs127() {
        // The stored format: these values are in the database and the v2 column default.
        assertEquals(1, Weekdays.bit(MONDAY))
        assertEquals(64, Weekdays.bit(SUNDAY))
        assertEquals(127, Weekdays.toMask(Weekdays.EveryDay))
        assertEquals(Weekdays.EVERY_DAY_MASK, Weekdays.toMask(Weekdays.EveryDay))
        assertEquals(Weekdays.EveryDay, Weekdays.fromMask(127))
        assertEquals(setOf(MONDAY, FRIDAY), Weekdays.fromMask(1 or 16))
    }

    @Test
    fun mask_everySetRoundTrips_throughTheRoomConverter() {
        val converters = Converters()
        (0..127).forEach { mask ->
            val days = converters.fromWeekdayMask(mask)
            assertEquals(mask, converters.toWeekdayMask(days))
        }
    }

    @Test
    fun week_followsTheGivenFirstDay() {
        assertEquals(DayOfWeek.entries, weekStartingOn(MONDAY))
        assertEquals(listOf(SUNDAY) + DayOfWeek.entries.dropLast(1), weekStartingOn(SUNDAY))
    }

    @Test
    fun format_namesTheDaysInWeekOrder() {
        val uk = Locale.UK
        assertEquals("Every day", formatWeekdays(Weekdays.EveryDay, MONDAY, uk))
        assertEquals("No days", formatWeekdays(emptySet(), MONDAY, uk))
        assertEquals("Mon, Fri", formatWeekdays(setOf(FRIDAY, MONDAY), MONDAY, uk))
        assertEquals("Mon, Sat, Sun", formatWeekdays(setOf(SUNDAY, MONDAY, SATURDAY), MONDAY, uk))
        assertEquals("Sun, Mon, Sat", formatWeekdays(setOf(SUNDAY, MONDAY, SATURDAY), SUNDAY, uk))
    }
}
