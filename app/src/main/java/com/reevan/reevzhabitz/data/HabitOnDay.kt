package com.reevan.reevzhabitz.data

import androidx.room.Embedded

/** A habit together with whether it was done on the day it was queried for. */
data class HabitOnDay(
    @Embedded val habit: Habit,
    val done: Boolean,
)
