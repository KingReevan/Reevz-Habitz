package com.reevan.reevzhabitz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.ui.ReevzHabitzApp
import com.reevan.reevzhabitz.ui.settings.PreferencesViewModel
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme
import com.reevan.reevzhabitz.util.TodayClock
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restarted on every return to the foreground, so "today" is re-read after the phone has
        // slept through midnight, then kept ticking while the app stays on screen.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                TodayClock.instance.tickAtMidnight()
            }
        }

        setContent {
            // Same instance a Settings screen would use, so a theme change applies immediately.
            val preferences: PreferencesViewModel =
                viewModel(factory = PreferencesViewModel.Factory)
            val settings by preferences.settings.collectAsStateWithLifecycle()

            ReevzHabitzTheme(themeMode = settings.themeMode) {
                ReevzHabitzApp()
            }
        }
    }
}
