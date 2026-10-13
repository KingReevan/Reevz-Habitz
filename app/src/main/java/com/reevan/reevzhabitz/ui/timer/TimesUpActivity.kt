package com.reevan.reevzhabitz.ui.timer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.timer.TimerController
import com.reevan.reevzhabitz.timer.TimerState
import com.reevan.reevzhabitz.ui.settings.PreferencesViewModel
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme
import com.reevan.reevzhabitz.ui.theme.isDark

/**
 * The "Time's up" page that wakes the phone when a timer rings with the screen off or locked,
 * like the Clock app's. Android opens it through the ringing notification's full-screen intent;
 * while the phone is in use it shows the pop-up instead.
 *
 * It turns the screen on and shows over the lock screen without unlocking it, so it shows only the
 * ringing timer (Stop, +1:00, the overtime), never the habits. It closes itself as soon as the
 * timer stops ringing, from here or from the notification's buttons.
 */
class TimesUpActivity : ComponentActivity() {

    private val preferences: PreferencesViewModel by viewModels { PreferencesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        val timer = TimerController.get(this)
        setContent {
            val settings by preferences.settings.collectAsStateWithLifecycle()
            val state by timer.state.collectAsStateWithLifecycle()
            LaunchedEffect(state) {
                if (state !is TimerState.Ringing) finish()
            }
            val themeMode = settings?.themeMode ?: ThemeMode.DARK
            // Status bar icons follow the app's theme, not the phone's, as in MainActivity.
            val dark = themeMode.isDark
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ReevzHabitzTheme(themeMode = themeMode) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    val ringing = state as? TimerState.Ringing
                    if (ringing != null) {
                        TimerCountdown(
                            state = ringing,
                            timer = timer,
                            modifier = Modifier.safeDrawingPadding(),
                        )
                    }
                }
            }
        }
    }

    /** Lights the screen and shows above the lock screen, kept lit while it rings. */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
