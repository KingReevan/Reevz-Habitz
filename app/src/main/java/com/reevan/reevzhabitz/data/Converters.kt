package com.reevan.reevzhabitz.data

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Dates are stored as epoch days: a plain calendar day with no time or zone, so "done on
 * 3 October" stays 3 October even if the phone later changes time zone.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    /** A habit's weekdays, stored as a mask (see [Weekdays]). */
    @TypeConverter
    fun fromWeekdayMask(mask: Int): Set<DayOfWeek> = Weekdays.fromMask(mask)

    @TypeConverter
    fun toWeekdayMask(days: Set<DayOfWeek>): Int = Weekdays.toMask(days)
}
