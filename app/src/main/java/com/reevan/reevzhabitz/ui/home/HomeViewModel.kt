package com.reevan.reevzhabitz.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.AppSettingsDao
import com.reevan.reevzhabitz.data.Completion
import com.reevan.reevzhabitz.data.CompletionDao
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.util.TodayClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    /** Today's habits in display order: to do first, done last. */
    val habits: List<HabitOnDay>,
    val sort: HomeSort,
)

/**
 * Today's habits and their ticks. When [today] moves on at midnight the query switches to the new
 * day, which has no completions yet — that is the whole "reset": every habit starts it unticked.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao,
    private val settingsDao: AppSettingsDao,
    private val today: StateFlow<LocalDate>,
) : ViewModel() {

    /** Null until the first read completes, so Home doesn't flash its empty message. */
    val state: StateFlow<HomeUiState?> =
        combine(
            today.flatMapLatest { habitDao.observeDueOn(it) },
            settingsDao.observe().map { it?.homeSort ?: HomeSort.ALPHABETICAL },
        ) { habits, sort ->
            HomeUiState(habits = orderForHome(habits, sort), sort = sort)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    fun markDone(habitId: Long) {
        val day = today.value
        viewModelScope.launch { completionDao.markDone(Completion(habitId, day)) }
    }

    fun markNotDone(habitId: Long) {
        val day = today.value
        viewModelScope.launch { completionDao.markNotDone(habitId, day) }
    }

    fun cycleSort() {
        viewModelScope.launch { settingsDao.cycleHomeSort() }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val db = HabitDatabase.getInstance(application)
                HomeViewModel(
                    habitDao = db.habitDao(),
                    completionDao = db.completionDao(),
                    settingsDao = db.appSettingsDao(),
                    today = TodayClock.instance.today,
                )
            }
        }
    }
}
