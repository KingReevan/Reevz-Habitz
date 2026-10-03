package com.reevan.reevzhabitz.ui.addhabit

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.ui.habitform.HabitDraft
import com.reevan.reevzhabitz.util.TodayClock
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Saves a new habit. Holds no form state: ViewModels here are activity-scoped, so form state kept
 * in one would reappear the next time Add Habit opens. The form lives in the screen instead, saved
 * with it, and is gone once the screen closes.
 */
class AddHabitViewModel(
    private val habitDao: HabitDao,
    private val today: StateFlow<LocalDate>,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    fun create(draft: HabitDraft, onCreated: () -> Unit) {
        val habit = draft.toHabit(today = today.value, createdAt = now())
        viewModelScope.launch {
            habitDao.insert(habit)
            onCreated()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                AddHabitViewModel(
                    habitDao = HabitDatabase.getInstance(application).habitDao(),
                    today = TodayClock.instance.today,
                )
            }
        }
    }
}
