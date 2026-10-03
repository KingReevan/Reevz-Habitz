package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.util.durationUntilNextDay
import com.reevan.reevzhabitz.util.formatHeaderDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class DatesTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")

    private fun at(text: String, zone: ZoneId = kolkata): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.parse(text), zone)

    @Test
    fun headerDate_isZeroPaddedDayAndFullMonth() {
        assertEquals("03 October", formatHeaderDate(LocalDate.of(2026, 10, 3), Locale.ENGLISH))
        assertEquals("25 December", formatHeaderDate(LocalDate.of(2026, 12, 25), Locale.ENGLISH))
    }

    @Test
    fun untilNextDay_fromLateEvening() {
        assertEquals(Duration.ofHours(1), durationUntilNextDay(at("2026-10-03T23:00")))
    }

    @Test
    fun untilNextDay_atExactlyMidnight_isAWholeDayNotZero() {
        assertEquals(Duration.ofDays(1), durationUntilNextDay(at("2026-10-03T00:00")))
    }

    @Test
    fun untilNextDay_oneSecondBeforeMidnight() {
        assertEquals(Duration.ofSeconds(1), durationUntilNextDay(at("2026-10-03T23:59:59")))
    }

    @Test
    fun untilNextDay_acrossYearEnd() {
        assertEquals(Duration.ofMinutes(30), durationUntilNextDay(at("2026-12-31T23:30")))
    }

    @Test
    fun untilNextDay_onDstSpringForwardDay_isShorter() {
        // London loses an hour at 01:00 on 29 March 2026, so that day is 23 hours long.
        val london = ZoneId.of("Europe/London")
        assertEquals(Duration.ofHours(23), durationUntilNextDay(at("2026-03-29T00:00", london)))
    }

    @Test
    fun untilNextDay_whenDstSkipsMidnight_targetsTheDaysRealStart() {
        // On 4 Nov 2018 São Paulo jumped from 23:59:59 straight to 01:00, so the day began at
        // 01:00 local — one real hour after 23:00 the night before.
        val saoPaulo = ZoneId.of("America/Sao_Paulo")
        assertEquals(Duration.ofHours(1), durationUntilNextDay(at("2018-11-03T23:00", saoPaulo)))
    }
}
