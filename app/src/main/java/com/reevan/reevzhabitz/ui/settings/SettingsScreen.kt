package com.reevan.reevzhabitz.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

/** Settings: the theme picker and "Clear deleted stats". */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    preferences: PreferencesViewModel = viewModel(factory = PreferencesViewModel.Factory),
    deletedStats: DeletedStatsViewModel = viewModel(factory = DeletedStatsViewModel.Factory),
) {
    val settings by preferences.settings.collectAsStateWithLifecycle()
    val deletedCount by deletedStats.deletedCount.collectAsStateWithLifecycle()
    // MainActivity doesn't draw until settings have loaded, so this is never null in practice.
    val selected = settings?.themeMode ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        ThemePicker(selected = selected, onSelect = preferences::setThemeMode)
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        ClearDeletedStats(count = deletedCount, onClear = deletedStats::clearDeletedStats)
    }
}

/**
 * Permanently deletes every removed habit whose stats were kept. Says how many first, and asks.
 * Disabled when there is nothing to clear.
 */
@Composable
private fun ClearDeletedStats(count: Int?, onClear: () -> Unit) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val enabled = (count ?: 0) > 0

    SectionTitle("Data")
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button) { confirming = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = "Clear deleted stats",
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
        Text(
            text = when (count) {
                null -> ""
                0 -> "No deleted habits have stats kept."
                1 -> "1 deleted habit still has its stats kept."
                else -> "$count deleted habits still have their stats kept."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirming && enabled) {
        val habits = if (count == 1) "1 deleted habit" else "$count deleted habits"
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Clear deleted stats?") },
            text = {
                Text(
                    "All statistics of $habits will be cleared completely. They will no longer " +
                        "appear in Statistics. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        confirming = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel") }
            },
        )
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
