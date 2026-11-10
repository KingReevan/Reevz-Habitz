package com.reevan.reevzhabitz.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/** The phone's locale, read so the screen recomposes if the language or region changes. */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * The day the phone's region starts its week on. The Statistics calendar and the Advanced options
 * day picker both follow it, so their weekdays line up.
 */
@Composable
fun firstDayOfWeek(): DayOfWeek = WeekFields.of(currentLocale()).firstDayOfWeek
