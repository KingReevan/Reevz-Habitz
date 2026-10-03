package com.reevan.reevzhabitz

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.colorScheme
import com.reevan.reevzhabitz.ui.theme.habitzColors
import com.reevan.reevzhabitz.ui.theme.isDark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards legibility across all four themes, using WCAG contrast ratios: 4.5 for body text, 3.0
 * for icons, large text and other graphics.
 */
class ThemeColorsTest {

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun assertContrast(min: Double, a: Color, b: Color, what: String) {
        val ratio = contrast(a, b)
        assertTrue("$what: %.2f < %.1f".format(ratio, min), ratio >= min)
    }

    @Test
    fun everyTheme_textIsReadable() {
        ThemeMode.entries.forEach { mode ->
            val s = mode.colorScheme()
            assertContrast(4.5, s.onBackground, s.background, "$mode onBackground")
            assertContrast(4.5, s.onSurface, s.surface, "$mode onSurface")
            assertContrast(4.5, s.onSurfaceVariant, s.surface, "$mode onSurfaceVariant")
            assertContrast(4.5, s.onPrimary, s.primary, "$mode onPrimary")
            assertContrast(4.5, s.primary, s.surfaceContainerHigh, "$mode primary on container")
            assertContrast(4.5, s.onSurface, s.surfaceContainerHigh, "$mode dialog text")
            assertContrast(4.5, s.onError, s.error, "$mode onError")
        }
    }

    @Test
    fun everyTheme_statusColoursStandOutFromTheBackground() {
        ThemeMode.entries.forEach { mode ->
            val bg = mode.colorScheme().background
            val extra = mode.habitzColors()
            assertContrast(3.0, extra.done, bg, "$mode done")
            assertContrast(3.0, extra.missed, bg, "$mode missed")
            assertContrast(1.5, extra.todayRing, bg, "$mode today ring")
            assertEquals(mode.isDark, extra.isDark)
        }
    }

    @Test
    fun habitColours_whiteIconsAndNamesAreReadableInEveryTheme() {
        HabitColors.all.forEach { color ->
            ThemeMode.entries.forEach { mode ->
                val shade = if (mode.isDark) color.dark else color.light
                val bg = mode.colorScheme().background
                assertContrast(3.0, Color.White, shade, "${color.key} icon in $mode")
                assertContrast(4.5, shade, bg, "${color.key} name in $mode")
            }
        }
    }

    @Test
    fun habitColours_keysAreUniqueAndUnknownKeysFallBack() {
        val keys = HabitColors.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertSame(HabitColors.all.first(), HabitColors.forKey("no-such-colour"))
        assertSame(HabitColors.all[3], HabitColors.forKey(keys[3]))
    }

    @Test
    fun habitIcons_keysAndDrawablesAreUniqueAndUnknownKeysFallBack() {
        val icons = HabitIcons.all
        assertEquals(icons.size, icons.map { it.key }.toSet().size)
        assertEquals(icons.size, icons.map { it.drawable }.toSet().size)
        assertTrue("Expected a wide icon set", icons.size >= 90)
        assertSame(icons.first(), HabitIcons.forKey("no-such-icon"))
    }
}
