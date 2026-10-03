package com.reevan.reevzhabitz.ui.common

import com.reevan.reevzhabitz.data.AppSettingsDao
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HomeSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Every active habit — including ones that haven't started yet — in Home's current sort. The list
 * behind Remove Habit and Edit Habit, so both show habits in the same places Home does.
 */
fun activeHabitsInHomeOrder(habitDao: HabitDao, settingsDao: AppSettingsDao): Flow<List<Habit>> =
    combine(
        habitDao.observeActive(),
        settingsDao.observe().map { it?.homeSort ?: HomeSort.ALPHABETICAL },
    ) { habits, sort -> habits.sortedFor(sort) }
