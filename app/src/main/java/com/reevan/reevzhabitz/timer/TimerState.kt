package com.reevan.reevzhabitz.timer

import java.util.Locale

/**
 * The countdown timer as plain data. Every time here is on the since-boot clock
 * (`SystemClock.elapsedRealtime`): it counts time the phone spends asleep and never jumps when the
 * wall clock or time zone changes. Callers pass the current reading in as `now`, so every rule in
 * this file is pure.
 */
sealed interface TimerState {
    /** Nothing set going: the keypad shows. */
    data object Idle : TimerState

    /** Counting down to [endsAt]. [total] is the length the ring measures against. */
    data class Running(val endsAt: Long, val total: Long) : TimerState

    /** Stopped with [left] to go. */
    data class Paused(val left: Long, val total: Long) : TimerState

    /** Ran out at [since], and rings until stopped. [total] is the length that ran out. */
    data class Ringing(val since: Long, val total: Long) : TimerState
}

const val ONE_MINUTE: Long = 60_000

fun startTimer(duration: Long, now: Long): TimerState = TimerState.Running(now + duration, duration)

fun TimerState.paused(now: Long): TimerState =
    if (this is TimerState.Running) TimerState.Paused((endsAt - now).coerceAtLeast(0), total) else this

fun TimerState.resumed(now: Long): TimerState =
    if (this is TimerState.Paused) TimerState.Running(now + left, total) else this

/** The +1:00 button. While ringing it silences the alarm and runs one more minute. */
fun TimerState.plusMinute(now: Long): TimerState = when (this) {
    is TimerState.Running -> TimerState.Running(endsAt + ONE_MINUTE, total + ONE_MINUTE)
    is TimerState.Paused -> TimerState.Paused(left + ONE_MINUTE, total + ONE_MINUTE)
    is TimerState.Ringing -> TimerState.Running(now + ONE_MINUTE, ONE_MINUTE)
    TimerState.Idle -> this
}

/** A running timer whose end has passed starts ringing; anything else stays as it is. */
fun TimerState.settled(now: Long): TimerState =
    if (this is TimerState.Running && now >= endsAt) TimerState.Ringing(endsAt, total) else this

/** Time left, in milliseconds. Negative while ringing: how long ago it ran out. */
fun TimerState.left(now: Long): Long = when (this) {
    is TimerState.Running -> endsAt - now
    is TimerState.Paused -> left
    is TimerState.Ringing -> -(now - since)
    TimerState.Idle -> 0
}

/** How much of the ring is still to go, from 1 (just started) down to 0 (ran out). */
fun TimerState.fractionLeft(now: Long): Float {
    val total = when (this) {
        is TimerState.Running -> total
        is TimerState.Paused -> total
        is TimerState.Ringing -> return 0f
        TimerState.Idle -> return 1f
    }
    return (left(now).toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
}

/**
 * A timer reading like the Clock app's: "5:00", "1:05:00". Time left rounds up, so a fresh
 * five-minute timer shows 5:00 and it rings as it reaches 0:00. Overtime (negative) counts up from
 * "-0:00".
 */
fun formatTimer(millis: Long): String {
    val over = millis < 0
    val seconds = if (over) -millis / 1000 else (millis + 999) / 1000
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    val body = if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%d:%02d", m, s)
    }
    return if (over) "-$body" else body
}

/**
 * What the keypad has typed, like the Clock app's: up to six digits that fill "hh mm ss" from the
 * right, so typing 5, 3, 0 reads 00h 05m 30s. Each pair may exceed 59 (99 seconds is fine, as in
 * the Clock app); the duration simply adds them up.
 */
data class TimerDigits(val digits: String = "") {

    init {
        require(digits.length <= MAX && digits.all(Char::isDigit) && !digits.startsWith('0'))
    }

    /** Types a key: "0" to "9", or "00". Leading zeros and digits past six are ignored. */
    fun type(key: String): TimerDigits {
        var next = digits
        for (c in key) {
            if (next.length == MAX || (next.isEmpty() && c == '0')) continue
            next += c
        }
        return TimerDigits(next)
    }

    fun backspace(): TimerDigits = TimerDigits(digits.dropLast(1))

    private val padded: String get() = digits.padStart(MAX, '0')
    val hours: String get() = padded.substring(0, 2)
    val minutes: String get() = padded.substring(2, 4)
    val seconds: String get() = padded.substring(4, 6)

    val isEmpty: Boolean get() = digits.isEmpty()

    val duration: Long
        get() = (hours.toLong() * 3600 + minutes.toLong() * 60 + seconds.toLong()) * 1000

    private companion object {
        const val MAX = 6
    }
}
