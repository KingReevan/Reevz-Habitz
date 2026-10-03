package com.reevan.reevzhabitz.ui.removehabit

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.AppSettingsDao
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.common.sortedFor
import com.reevan.reevzhabitz.util.TodayClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Every active habit — including ones that haven't started yet — in Home's current sort, and the
 * removal itself. Holds no selection: that is per-visit state, kept in the screen (see
 * AddHabitViewModel for why).
 */
class RemoveHabitViewModel(
    private val habitDao: HabitDao,
    settingsDao: AppSettingsDao,
    private val today: StateFlow<LocalDate>,
) : ViewModel() {

    /** Null until the first read completes. */
    val habits: StateFlow<List<Habit>?> =
        combine(
            habitDao.observeActive(),
            settingsDao.observe().map { it?.homeSort ?: HomeSort.ALPHABETICAL },
        ) { habits, sort -> habits.sortedFor(sort) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun remove(ids: List<Long>, keepStats: Boolean, onRemoved: () -> Unit) {
        val day = today.value
        viewModelScope.launch {
            habitDao.remove(ids, keepStats, day)
            onRemoved()
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val db = HabitDatabase.getInstance(application)
                RemoveHabitViewModel(
                    habitDao = db.habitDao(),
                    settingsDao = db.appSettingsDao(),
                    today = TodayClock.instance.today,
                )
            }
        }
    }
}
