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
import com.reevan.reevzhabitz.ui.common.SubmissionTracker
import com.reevan.reevzhabitz.ui.common.activeHabitsInHomeOrder
import com.reevan.reevzhabitz.util.TodayClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
        activeHabitsInHomeOrder(habitDao, settingsDao)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Remove's submissions; the screen watches it to leave once the habits are gone. */
    val submissions = SubmissionTracker()

    fun remove(token: String, ids: List<Long>, keepStats: Boolean) {
        val day = today.value
        viewModelScope.launch {
            habitDao.remove(ids, keepStats, day)
            submissions.complete(token)
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
