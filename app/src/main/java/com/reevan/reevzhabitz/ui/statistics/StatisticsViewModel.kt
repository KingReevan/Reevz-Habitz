package com.reevan.reevzhabitz.ui.statistics

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.AppSettingsDao
import com.reevan.reevzhabitz.data.CompletionDao
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.common.sortedFor
import com.reevan.reevzhabitz.util.TodayClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StatisticsListState(
    /** Habits that have started, in Home's sort. */
    val active: List<Habit>,
    /** Removed habits with stats kept that had started before removal, in Home's sort. */
    val deleted: List<Habit>,
)

data class HabitStatsState(
    val habit: Habit,
    val history: HabitHistory,
)

/**
 * Statistics' list and each habit's history. Holds no per-visit state — the month on show lives
 * in the screen — so one activity-scoped instance serves every habit.
 */
class StatisticsViewModel(
    private val habitDao: HabitDao,
    private val completionDao: CompletionDao,
    settingsDao: AppSettingsDao,
    private val today: StateFlow<LocalDate>,
) : ViewModel() {

    /**
     * Habits not yet started are left out: they have nothing to show. So are deleted habits that
     * were removed before they ever started.
     */
    val list: StateFlow<StatisticsListState?> =
        combine(
            habitDao.observeActive(),
            habitDao.observeDeleted(),
            settingsDao.observe().map { it?.homeSort ?: HomeSort.ALPHABETICAL },
            today,
        ) { active, deleted, sort, day ->
            StatisticsListState(
                active = active.filter { !it.startDate.isAfter(day) }.sortedFor(sort),
                deleted = deleted.filter { !it.startDate.isAfter(it.deletedOn) }.sortedFor(sort),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** One habit with its full history, updating live. Emits null if the habit is gone. */
    fun habitStats(habitId: Long): Flow<HabitStatsState?> =
        combine(
            habitDao.observeById(habitId),
            completionDao.observeDatesFor(habitId),
            today,
        ) { habit, doneDays, day ->
            habit?.let {
                HabitStatsState(
                    habit = it,
                    history = HabitHistory(
                        startDate = it.startDate,
                        createdOn = Instant.ofEpochMilli(it.createdAt)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate(),
                        deletedOn = it.deletedOn,
                        doneDays = doneDays,
                        today = day,
                    ),
                )
            }
        }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val db = HabitDatabase.getInstance(application)
                StatisticsViewModel(
                    habitDao = db.habitDao(),
                    completionDao = db.completionDao(),
                    settingsDao = db.appSettingsDao(),
                    today = TodayClock.instance.today,
                )
            }
        }
    }
}
