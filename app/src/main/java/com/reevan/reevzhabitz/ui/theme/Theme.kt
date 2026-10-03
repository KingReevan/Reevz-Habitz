package com.reevan.reevzhabitz.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.reevan.reevzhabitz.data.ThemeMode

/** Whether this theme has a dark background. Drives habit colour variants and system bar icons. */
val ThemeMode.isDark: Boolean
    get() = this != ThemeMode.LIGHT

/** The Material scheme for a theme. Exposed so Settings can preview themes that aren't active. */
fun ThemeMode.colorScheme(): ColorScheme = when (this) {
    ThemeMode.LIGHT -> LightScheme
    ThemeMode.DARK -> DarkScheme
    ThemeMode.VSCODE_DARK -> VsCodeDarkScheme
    ThemeMode.TOKYO_NIGHT -> TokyoNightScheme
}

/** The app-specific colours for a theme. */
fun ThemeMode.habitzColors(): HabitzColors = when (this) {
    ThemeMode.LIGHT -> LightHabitz
    ThemeMode.DARK -> DarkHabitz
    ThemeMode.VSCODE_DARK -> VsCodeDarkHabitz
    ThemeMode.TOKYO_NIGHT -> TokyoNightHabitz
}

private val LocalHabitzColors = staticCompositionLocalOf { DarkHabitz }

/** Entry point for the colours Material 3 has no role for, alongside `MaterialTheme`. */
object HabitzTheme {
    val colors: HabitzColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHabitzColors.current
}

/**
 * The app theme. One of four fixed palettes chosen in Settings.
 *
 * **Material You dynamic colour is deliberately off.** The themes are named palettes (VS Code
 * Dark, Tokyo Night), and a wallpaper-derived scheme would repaint them in whatever the phone's
 * background happens to be. The two cannot both be in charge; the named palette wins.
 */
@Composable
fun ReevzHabitzTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalHabitzColors provides themeMode.habitzColors()) {
        MaterialTheme(
            colorScheme = themeMode.colorScheme(),
            typography = Typography,
            content = content,
        )
    }
}
