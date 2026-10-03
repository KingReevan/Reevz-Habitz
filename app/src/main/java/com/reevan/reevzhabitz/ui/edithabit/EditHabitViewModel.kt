package com.reevan.reevzhabitz.ui.edithabit

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
import com.reevan.reevzhabitz.ui.habitform.HabitEdits
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Edit Habit's list and saving. Holds no editor state: the edits live in the screen, saved with
 * it (see AddHabitViewModel for why), so leaving and reopening an editor always starts from what
 * is stored.
 */
class EditHabitViewModel(
    private val habitDao: HabitDao,
    settingsDao: AppSettingsDao,
) : ViewModel() {

    /** Every active habit, including ones not yet started, in Home's sort. Null until loaded. */
    val habits: StateFlow<List<Habit>?> =
        activeHabitsInHomeOrder(habitDao, settingsDao)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** The habit as stored now, or null if it has gone (removed meanwhile). */
    suspend fun load(habitId: Long): Habit? =
        habitDao.getById(habitId)?.takeIf { it.deletedOn == null }

    /** Save's submissions; the editor watches it to leave once the change is stored. */
    val submissions = SubmissionTracker()

    fun save(token: String, original: Habit, edits: HabitEdits) {
        val updated = edits.applyTo(original)
        viewModelScope.launch {
            habitDao.update(updated)
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
                EditHabitViewModel(habitDao = db.habitDao(), settingsDao = db.appSettingsDao())
            }
        }
    }
}
