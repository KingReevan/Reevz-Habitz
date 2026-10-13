package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.timer.ONE_MINUTE
import com.reevan.reevzhabitz.timer.TimerDigits
import com.reevan.reevzhabitz.timer.TimerState
import com.reevan.reevzhabitz.timer.formatTimer
import com.reevan.reevzhabitz.timer.fractionLeft
import com.reevan.reevzhabitz.timer.left
import com.reevan.reevzhabitz.timer.paused
import com.reevan.reevzhabitz.timer.plusMinute
import com.reevan.reevzhabitz.timer.resumed
import com.reevan.reevzhabitz.timer.settled
import com.reevan.reevzhabitz.timer.startTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {

    private val fiveMinutes = 5 * ONE_MINUTE

    @Test
    fun start_countsDownFromNow() {
        val running = startTimer(fiveMinutes, now = 1_000)
        assertEquals(TimerState.Running(endsAt = 1_000 + fiveMinutes, total = fiveMinutes), running)
        assertEquals(fiveMinutes, running.left(1_000))
        assertEquals(ONE_MINUTE, running.left(1_000 + 4 * ONE_MINUTE))
        assertEquals(0.2f, running.fractionLeft(1_000 + 4 * ONE_MINUTE), 0.0001f)
    }

    @Test
    fun pause_freezesTheTimeLeft_andResumeCarriesOn() {
        val paused = startTimer(fiveMinutes, now = 0).paused(now = 2 * ONE_MINUTE)
        assertEquals(TimerState.Paused(left = 3 * ONE_MINUTE, total = fiveMinutes), paused)
        // Time passing while paused changes nothing.
        assertEquals(3 * ONE_MINUTE, paused.left(now = 99 * ONE_MINUTE))

        val resumed = paused.resumed(now = 10 * ONE_MINUTE)
        assertEquals(TimerState.Running(endsAt = 13 * ONE_MINUTE, total = fiveMinutes), resumed)
    }

    @Test
    fun plusMinute_addsToTheTimeAndTheRing() {
        val running = startTimer(fiveMinutes, now = 0)
        assertEquals(
            TimerState.Running(endsAt = 6 * ONE_MINUTE, total = 6 * ONE_MINUTE),
            running.plusMinute(now = 0),
        )
        assertEquals(
            TimerState.Paused(left = 2 * ONE_MINUTE, total = 6 * ONE_MINUTE),
            TimerState.Paused(left = ONE_MINUTE, total = fiveMinutes).plusMinute(now = 0),
        )
    }

    @Test
    fun plusMinute_whileRinging_runsOneMoreMinute() {
        val ringing = TimerState.Ringing(since = 1_000, total = fiveMinutes)
        assertEquals(
            TimerState.Running(endsAt = 9_000 + ONE_MINUTE, total = ONE_MINUTE),
            ringing.plusMinute(now = 9_000),
        )
    }

    @Test
    fun settled_ringsOnlyOnceTheEndHasPassed() {
        val running = startTimer(fiveMinutes, now = 0)
        assertSame(running, running.settled(now = fiveMinutes - 1))
        assertEquals(TimerState.Ringing(since = fiveMinutes, total = fiveMinutes), running.settled(fiveMinutes))
        // A late alarm still records when time actually ran out.
        assertEquals(
            TimerState.Ringing(since = fiveMinutes, total = fiveMinutes),
            running.settled(now = fiveMinutes + 7_000),
        )
        val paused = TimerState.Paused(left = 0, total = fiveMinutes)
        assertSame(paused, paused.settled(now = Long.MAX_VALUE))
    }

    @Test
    fun ringing_countsTimeOverAsNegative() {
        val ringing = TimerState.Ringing(since = 10_000, total = fiveMinutes)
        assertEquals(-4_000, ringing.left(now = 14_000))
        assertEquals("-0:04", formatTimer(ringing.left(now = 14_000)))
    }

    @Test
    fun actionsThatDoNotApply_changeNothing() {
        val idle = TimerState.Idle
        assertSame(idle, idle.paused(0))
        assertSame(idle, idle.resumed(0))
        assertSame(idle, idle.plusMinute(0))
        val paused = TimerState.Paused(left = 1, total = 2)
        assertSame(paused, paused.paused(0))
    }

    @Test
    fun format_roundsTimeLeftUp_likeAClock() {
        assertEquals("5:00", formatTimer(fiveMinutes))
        assertEquals("5:00", formatTimer(fiveMinutes - 1))   // a moment after starting
        assertEquals("0:01", formatTimer(1))
        assertEquals("0:00", formatTimer(0))
        assertEquals("1:05:00", formatTimer(65 * ONE_MINUTE))
        assertEquals("100:00:00", formatTimer(100 * 60 * ONE_MINUTE))
        assertEquals("-0:00", formatTimer(-999))
        assertEquals("-1:00", formatTimer(-ONE_MINUTE))
    }

    @Test
    fun digits_fillFromTheRight() {
        val typed = TimerDigits().type("5").type("3").type("0")
        assertEquals("00", typed.hours)
        assertEquals("05", typed.minutes)
        assertEquals("30", typed.seconds)
        assertEquals(5 * ONE_MINUTE + 30_000, typed.duration)
    }

    @Test
    fun digits_ignoreLeadingZerosAndStopAtSix() {
        assertTrue(TimerDigits().type("0").type("00").isEmpty)
        assertEquals("100", TimerDigits().type("1").type("00").digits)
        val full = TimerDigits().type("1").type("2").type("3").type("4").type("5").type("6")
        assertEquals("123456", full.type("7").digits)
        // "00" with only one place left adds a single zero.
        assertEquals("123450", TimerDigits("12345").type("00").digits)
    }

    @Test
    fun digits_backspaceRemovesTheLastTyped() {
        assertEquals("53", TimerDigits("530").backspace().digits)
        assertTrue(TimerDigits().backspace().isEmpty)
    }

    @Test
    fun digits_pairsOver59_simplyAddUp() {
        // 99 seconds, as the Clock app allows: a minute and 39 seconds.
        assertEquals(99_000L, TimerDigits("99").duration)
        assertEquals((99 * 3600 + 99 * 60 + 99) * 1000L, TimerDigits("999999").duration)
    }
}
