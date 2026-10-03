package com.reevan.reevzhabitz.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The Home header's date, e.g. "03 October". */
fun formatHeaderDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("dd MMMM", locale))

/**
 * Time from [now] until the next local day begins.
 *
 * Uses `atStartOfDay(zone)` rather than assuming 00:00, because in a few zones a DST change skips
 * midnight entirely and the day starts at 01:00. Always positive: at exactly midnight the answer is
 * the full day ahead, not zero.
 */
fun durationUntilNextDay(now: ZonedDateTime): Duration {
    val nextDayStart = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, nextDayStart)
}

/** A date in full for forms, e.g. "Sunday, 04 October 2026". */
fun formatLongDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", locale))

/*
 * Material 3's DatePicker works in UTC milliseconds at midnight, whatever the phone's zone. These
 * two convert to and from a plain LocalDate in UTC on both sides, so the day picked is the day
 * stored, with no zone offset able to shift it by one.
 */

fun LocalDate.toDatePickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun datePickerMillisToLocalDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
