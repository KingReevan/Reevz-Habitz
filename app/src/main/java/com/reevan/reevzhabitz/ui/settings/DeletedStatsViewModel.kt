package com.reevan.reevzhabitz.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.HabitDao
import com.reevan.reevzhabitz.data.HabitDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Settings' "Clear deleted stats". Kept apart from [PreferencesViewModel], which MainActivity also
 * holds for the theme and has no business deleting data.
 *
 * This and Remove Habit (with "Keep stats" off) are the only places the app deletes user data.
 */
class DeletedStatsViewModel(
    private val habitDao: HabitDao,
) : ViewModel() {

    /** Removed habits whose stats are still kept. Null until the first read. */
    val deletedCount: StateFlow<Int?> =
        habitDao.observeDeletedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun clearDeletedStats() {
        viewModelScope.launch { habitDao.purgeDeleted() }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                DeletedStatsViewModel(HabitDatabase.getInstance(application).habitDao())
            }
        }
    }
}
