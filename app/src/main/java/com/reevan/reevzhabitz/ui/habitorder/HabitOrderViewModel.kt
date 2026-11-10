package com.reevan.reevzhabitz.ui.habitorder

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.ui.common.HomePositionOrder
import com.reevan.reevzhabitz.ui.common.SubmissionTracker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Habit Order's list and Save. Holds no unsaved order: that is per-visit state, kept in the screen
 * (see AddHabitViewModel for why).
 */
class HabitOrderViewModel(private val habitDao: HabitDao) : ViewModel() {

    /** Every active habit, including ones not yet started, in Home's order. Null until loaded. */
    val habits: StateFlow<List<Habit>?> =
        habitDao.observeActive()
            .map { it.sortedWith(HomePositionOrder) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Save's submissions; the screen watches it to leave once the order is stored. */
    val submissions = SubmissionTracker()

    /** Stores [ids], top first, as Home's order. */
    fun save(token: String, ids: List<Long>) {
        viewModelScope.launch {
            habitDao.setHomeOrder(ids)
            submissions.complete(token)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                HabitOrderViewModel(HabitDatabase.getInstance(application).habitDao())
            }
        }
    }
}
