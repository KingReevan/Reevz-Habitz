package com.reevan.reevzhabitz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.ui.ReevzHabitzApp
import com.reevan.reevzhabitz.ui.settings.PreferencesViewModel
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
