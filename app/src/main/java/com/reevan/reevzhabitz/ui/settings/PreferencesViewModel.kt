package com.reevan.reevzhabitz.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.reevan.reevzhabitz.data.AppSettings
import com.reevan.reevzhabitz.data.AppSettingsDao
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide preferences.
 *
 * MainActivity needs the theme before anything renders, and `viewModel()` resolves to the
 * activity's store, so a future Settings screen sees this same instance.
 */
class PreferencesViewModel(
    private val dao: AppSettingsDao,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        dao.observe()
            .map { it ?: AppSettings() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AppSettings(),
            )

    fun setThemeMode(mode: ThemeMode) {
        update { it.copy(themeMode = mode) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = dao.get() ?: AppSettings()
            dao.upsert(transform(current))
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                PreferencesViewModel(
                    dao = HabitDatabase.getInstance(application).appSettingsDao(),
                )
            }
        }
    }
}
