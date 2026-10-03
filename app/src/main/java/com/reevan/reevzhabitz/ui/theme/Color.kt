package com.reevan.reevzhabitz.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * The four themes. Each is a full Material colour scheme — including the surfaceContainer roles
 * that dialogs, the date picker and menus read, so none of them falls back to Material's default
 * purple-tinted greys — plus the few app-specific colours Material has no role for.
 *
 * VS Code Dark follows VS Code's default dark theme (Dark+): #1E1E1E editor, #252526 side bar,
 * #3C3C3C borders, its blues and syntax accents. Tokyo Night follows the Night variant of the
 * Tokyo Night VS Code theme.
 */

/** App colours Material 3 has no role for. Read through `HabitzTheme.colors`. */
@Immutable
data class HabitzColors(
    /** Picks each habit colour's dark-theme variant. */
    val isDark: Boolean,
    /** The thin line between stacked habit cards. */
    val divider: Color,
    /** Statistics: a day the habit was done. */
    val done: Color,
    /** Statistics: a day the habit was due and not done. */
    val missed: Color,
    /** Statistics: a day still to come. */
    val future: Color,
    /** Statistics: the ring marking today. */
    val todayRing: Color,
)

// ---- Light ---------------------------------------------------------------------------------

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF1F5FD1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E4FF),
    onPrimaryContainer = Color(0xFF0A2A66),
    inversePrimary = Color(0xFFAFC6FF),
    secondary = Color(0xFF545F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE3F0),
    onSecondaryContainer = Color(0xFF1A2433),
    tertiary = Color(0xFF7A4F9E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0DBFF),
    onTertiaryContainer = Color(0xFF2E0A4D),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFECEDF1),
    onSurfaceVariant = Color(0xFF5A5D66),
    surfaceTint = Color(0xFF1F5FD1),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF8A8D96),
    outlineVariant = Color(0xFFD5D6DC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAFAFA),
    surfaceDim = Color(0xFFDCDCE0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F7),
    surfaceContainer = Color(0xFFF0F0F3),
    surfaceContainerHigh = Color(0xFFEAEAEE),
    surfaceContainerHighest = Color(0xFFE4E4E8),
)

internal val LightHabitz = HabitzColors(
    isDark = false,
    divider = Color(0xFFDCDCE0),
    done = Color(0xFF2E7D32),
    missed = Color(0xFFC62828),
    future = Color(0xFFD0D0D5),
    todayRing = Color(0xFFF9A825),
)

// ---- Dark ----------------------------------------------------------------------------------

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B2A5C),
    primaryContainer = Color(0xFF1F3F73),
    onPrimaryContainer = Color(0xFFD6E3FF),
    inversePrimary = Color(0xFF1F5FD1),
    secondary = Color(0xFFBFC6D6),
    onSecondary = Color(0xFF29313F),
    secondaryContainer = Color(0xFF3F4756),
    onSecondaryContainer = Color(0xFFDBE2F2),
    tertiary = Color(0xFFD7B9F5),
    onTertiary = Color(0xFF3B1F57),
    tertiaryContainer = Color(0xFF53376F),
    onTertiaryContainer = Color(0xFFF0DBFF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A2A2D),
    onSurfaceVariant = Color(0xFFB4B4BA),
    surfaceTint = Color(0xFF8AB4F8),
    inverseSurface = Color(0xFFE6E6E6),
    inverseOnSurface = Color(0xFF2F2F2F),
    error = Color(0xFFF28B82),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8E8E94),
    outlineVariant = Color(0xFF3A3A3D),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF363636),
    surfaceDim = Color(0xFF121212),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030),
)

internal val DarkHabitz = HabitzColors(
    isDark = true,
    divider = Color(0xFF2C2C2E),
    done = Color(0xFF66BB6A),
    missed = Color(0xFFEF5350),
    future = Color(0xFF3A3A3D),
    todayRing = Color(0xFFFDD835),
)

// ---- VS Code Dark --------------------------------------------------------------------------

internal val VsCodeDarkScheme: ColorScheme = darkColorScheme(
    // VS Code's link blue (#3794FF), lifted a touch to #409AFF so it clears 4.5:1 as text on the
    // menu button container. Not the #0E639C button blue: primary is also text on #1E1E1E,
    // where the button blue is too dark to read. Dark text on it keeps filled buttons legible.
    primary = Color(0xFF409AFF),
    onPrimary = Color(0xFF001A33),
    primaryContainer = Color(0xFF04395E),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF0E639C),
    secondary = Color(0xFF4EC9B0),
    onSecondary = Color(0xFF00261F),
    secondaryContainer = Color(0xFF0F4A40),
    onSecondaryContainer = Color(0xFFC8F5EA),
    tertiary = Color(0xFFC586C0),
    onTertiary = Color(0xFF2E0F2B),
    tertiaryContainer = Color(0xFF5A2D56),
    onTertiaryContainer = Color(0xFFF6D9F3),
    background = Color(0xFF1E1E1E),
    onBackground = Color(0xFFCCCCCC),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFCCCCCC),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFF9D9D9D),
    surfaceTint = Color(0xFF409AFF),
    inverseSurface = Color(0xFFCCCCCC),
    inverseOnSurface = Color(0xFF1E1E1E),
    error = Color(0xFFF48771),
    onError = Color(0xFF3A0E06),
    errorContainer = Color(0xFF5A1D1D),
    onErrorContainer = Color(0xFFF8C9C1),
    outline = Color(0xFF858585),
    outlineVariant = Color(0xFF3C3C3C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3C3C3C),
    surfaceDim = Color(0xFF1E1E1E),
    surfaceContainerLowest = Color(0xFF181818),
    surfaceContainerLow = Color(0xFF212121),
    surfaceContainer = Color(0xFF252526),
    surfaceContainerHigh = Color(0xFF2D2D30),
    surfaceContainerHighest = Color(0xFF3C3C3C),
)

internal val VsCodeDarkHabitz = HabitzColors(
    isDark = true,
    divider = Color(0xFF333333),
    done = Color(0xFF73C991),
    missed = Color(0xFFF14C4C),
    future = Color(0xFF3C3C3C),
    todayRing = Color(0xFFCCA700),
)

// ---- Tokyo Night ---------------------------------------------------------------------------

internal val TokyoNightScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF15161E),
    primaryContainer = Color(0xFF2E3C64),
    onPrimaryContainer = Color(0xFFC0CAF5),
    inversePrimary = Color(0xFF3D59A1),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF15161E),
    secondaryContainer = Color(0xFF3D2F5B),
    onSecondaryContainer = Color(0xFFE2D6FC),
    tertiary = Color(0xFF7DCFFF),
    onTertiary = Color(0xFF15161E),
    tertiaryContainer = Color(0xFF1F4A66),
    onTertiaryContainer = Color(0xFFD3EEFF),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF1A1B26),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF24283B),
    onSurfaceVariant = Color(0xFFA9B1D6),
    surfaceTint = Color(0xFF7AA2F7),
    inverseSurface = Color(0xFFC0CAF5),
    inverseOnSurface = Color(0xFF1A1B26),
    error = Color(0xFFF7768E),
    onError = Color(0xFF15161E),
    errorContainer = Color(0xFF4A2131),
    onErrorContainer = Color(0xFFFFC7D0),
    outline = Color(0xFF565F89),
    outlineVariant = Color(0xFF292E42),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2F334D),
    surfaceDim = Color(0xFF16161E),
    surfaceContainerLowest = Color(0xFF16161E),
    surfaceContainerLow = Color(0xFF1C1E2C),
    surfaceContainer = Color(0xFF1F2335),
    surfaceContainerHigh = Color(0xFF24283B),
    surfaceContainerHighest = Color(0xFF292E42),
)

internal val TokyoNightHabitz = HabitzColors(
    isDark = true,
    divider = Color(0xFF292E42),
    done = Color(0xFF9ECE6A),
    missed = Color(0xFFF7768E),
    future = Color(0xFF3B4261),
    todayRing = Color(0xFFE0AF68),
)
