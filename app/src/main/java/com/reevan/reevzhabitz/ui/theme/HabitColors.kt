package com.reevan.reevzhabitz.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * A colour a habit can be given. It is used twice: as the card's left edge, behind a pure-white
 * icon, and as the habit name's text colour on the screen background.
 *
 * Those two uses pull in opposite directions, so each colour has two shades:
 * - [light] for the Light theme: dark enough for the name to read on a near-white background.
 * - [dark] for the three dark themes: light enough for the name to read on a near-black
 *   background, yet still dark enough for the white icon to stand out on the edge.
 *
 * Generated from a hue and a target luminance, so every colour clears the same contrast bar;
 * `ThemeColorsTest` enforces it. [key] is what the database stores — never rename or remove one.
 */
data class HabitColor(
    val key: String,
    val label: String,
    val light: Color,
    val dark: Color,
)

/** This colour's shade for the current theme. */
val HabitColor.current: Color
    @Composable
    @ReadOnlyComposable
    get() = if (HabitzTheme.colors.isDark) dark else light

object HabitColors {

    val all: List<HabitColor> = listOf(
        HabitColor("red", "Red", light = Color(0xFFC61C16), dark = Color(0xFFEC5B56)),
        HabitColor("coral", "Coral", light = Color(0xFFBB320F), dark = Color(0xFFEF5B35)),
        HabitColor("orange", "Orange", light = Color(0xFFA34C09), dark = Color(0xFFE1680C)),
        HabitColor("amber", "Amber", light = Color(0xFF8D5B04), dark = Color(0xFFC27C05)),
        HabitColor("gold", "Gold", light = Color(0xFF7A6306), dark = Color(0xFFA88909)),
        HabitColor("olive", "Olive", light = Color(0xFF66691E), dark = Color(0xFF8D902A)),
        HabitColor("lime", "Lime", light = Color(0xFF506E14), dark = Color(0xFF6F981B)),
        HabitColor("green", "Green", light = Color(0xFF227525), dark = Color(0xFF2FA132)),
        HabitColor("emerald", "Emerald", light = Color(0xFF147344), dark = Color(0xFF1CA05E)),
        HabitColor("mint", "Mint", light = Color(0xFF1D7258), dark = Color(0xFF279E7A)),
        HabitColor("teal", "Teal", light = Color(0xFF0D726B), dark = Color(0xFF119C93)),
        HabitColor("cyan", "Cyan", light = Color(0xFF0A707F), dark = Color(0xFF0E99AF)),
        HabitColor("sky", "Sky", light = Color(0xFF0D6C9B), dark = Color(0xFF1194D5)),
        HabitColor("blue", "Blue", light = Color(0xFF1662C6), dark = Color(0xFF458CEA)),
        HabitColor("indigo", "Indigo", light = Color(0xFF4156DD), dark = Color(0xFF7484E7)),
        HabitColor("violet", "Violet", light = Color(0xFF6E46DE), dark = Color(0xFF9679E7)),
        HabitColor("purple", "Purple", light = Color(0xFF8F34D4), dark = Color(0xFFAF6FE1)),
        HabitColor("magenta", "Magenta", light = Color(0xFFA826B2), dark = Color(0xFFD358DC)),
        HabitColor("pink", "Pink", light = Color(0xFFBD1B77), dark = Color(0xFFE754A7)),
        HabitColor("rose", "Rose", light = Color(0xFFC5164B), dark = Color(0xFFEC5583)),
        // Muted and earthy tones.
        HabitColor("salmon", "Salmon", light = Color(0xFFB03F33), dark = Color(0xFFD26F64)),
        HabitColor("brown", "Brown", light = Color(0xFF895A3B), dark = Color(0xFFB87E58)),
        HabitColor("ochre", "Ochre", light = Color(0xFF845E26), dark = Color(0xFFB58234)),
        HabitColor("sand", "Sand", light = Color(0xFF756339), dark = Color(0xFFA2894E)),
        HabitColor("moss", "Moss", light = Color(0xFF596C34), dark = Color(0xFF7B9448)),
        HabitColor("sage", "Sage", light = Color(0xFF466D53), dark = Color(0xFF619873)),
        HabitColor("steel", "Steel", light = Color(0xFF3D6B7E), dark = Color(0xFF5693AE)),
        HabitColor("slate", "Slate", light = Color(0xFF536782), dark = Color(0xFF798DAA)),
        HabitColor("denim", "Denim", light = Color(0xFF4262AE), dark = Color(0xFF708BC9)),
        HabitColor("lavender", "Lavender", light = Color(0xFF6757B4), dark = Color(0xFF8E83C7)),
        HabitColor("plum", "Plum", light = Color(0xFF874F99), dark = Color(0xFFAA79BA)),
        HabitColor("mauve", "Mauve", light = Color(0xFF8E507F), dark = Color(0xFFB479A5)),
        HabitColor("wine", "Wine", light = Color(0xFFA0485E), dark = Color(0xFFC17588)),
        HabitColor("taupe", "Taupe", light = Color(0xFF706355), dark = Color(0xFF998978)),
        HabitColor("grey", "Grey", light = Color(0xFF656565), dark = Color(0xFF8C8C8C)),
    )

    private val byKey: Map<String, HabitColor> = all.associateBy { it.key }

    /** The colour for a stored key. An unknown key falls back rather than crashing a screen. */
    fun forKey(key: String): HabitColor = byKey[key] ?: all.first()
}
