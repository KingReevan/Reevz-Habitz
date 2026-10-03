package com.reevan.reevzhabitz.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme
import com.reevan.reevzhabitz.ui.theme.colorScheme
import com.reevan.reevzhabitz.ui.theme.habitzColors

/**
 * Settings. The theme picker for now; "Clear deleted stats" joins it in Phase 5.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory),
) {
    val settings by preferences.settings.collectAsStateWithLifecycle()
    // MainActivity doesn't draw until settings have loaded, so this is never null in practice.
    val selected = settings?.themeMode ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        ThemePicker(selected = selected, onSelect = preferences::setThemeMode)
    }
}

@Composable
private fun ThemePicker(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    SectionTitle("Theme")
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(horizontal = 16.dp),
            ) {
                // The row handles the click, so the button itself must not be a second target.
                RadioButton(selected = mode == selected, onClick = null)
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                ThemeSwatch(mode)
            }
        }
    }
}

/** A thumbnail of a theme — its background with its accent and status colours on it. */
@Composable
private fun ThemeSwatch(mode: ThemeMode) {
    val scheme = mode.colorScheme()
    val extra = mode.habitzColors()
    val shape = RoundedCornerShape(6.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(scheme.background, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        listOf(scheme.primary, scheme.secondary, extra.done, extra.missed).forEach { color ->
            Dot(color)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        Modifier
            .size(10.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun ThemePickerPreview() {
    ReevzHabitzTheme(ThemeMode.TOKYO_NIGHT) {
        Surface {
            Column { ThemePicker(selected = ThemeMode.TOKYO_NIGHT, onSelect = {}) }
        }
    }
}
