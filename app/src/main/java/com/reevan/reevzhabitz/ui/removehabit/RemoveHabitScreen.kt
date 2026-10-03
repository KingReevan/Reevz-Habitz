package com.reevan.reevzhabitz.ui.removehabit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.ui.common.HabitCard
import com.reevan.reevzhabitz.ui.common.HabitCardDivider
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.current

/**
 * Remove Habit: tick any number of habits, then Remove Habit(s) at the bottom right. A dialog
 * confirms, with a "Keep stats" switch (on by default). Done, it returns to the Menu via
 * [onRemoved].
 */
@Composable
fun RemoveHabitScreen(
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RemoveHabitViewModel = viewModel(factory = RemoveHabitViewModel.Factory),
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val all = habits ?: return
    if (all.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = "No habits to remove.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // A List rather than a Set: rememberSaveable can put a List of Longs in a Bundle.
    var selectedIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var confirming by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    // Ignore any selected id whose habit has since disappeared.
    val selected = all.filter { it.id in selectedIds }

    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            items(all, key = { it.id }) { habit ->
                val isSelected = habit.id in selectedIds
                SelectableHabitCard(
                    habit = habit,
                    selected = isSelected,
                    onToggle = {
                        selectedIds =
                            if (isSelected) selectedIds - habit.id else selectedIds + habit.id
                    },
                )
                HabitCardDivider()
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = if (selected.isEmpty()) "None selected" else "${selected.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { confirming = true },
                enabled = selected.isNotEmpty() && !removing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Remove Habit(s)")
            }
        }
    }

    if (confirming && selected.isNotEmpty()) {
        ConfirmRemoveDialog(
            habits = selected,
            onConfirm = { keepStats ->
                confirming = false
                removing = true
                viewModel.remove(selected.map { it.id }, keepStats, onRemoved)
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun SelectableHabitCard(
    habit: Habit,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val color = HabitColors.forKey(habit.colorKey)
    HabitCard(
        name = habit.name,
        color = color,
        icon = HabitIcons.forKey(habit.iconKey),
        description = habit.description,
        trailing = {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = color.current,
                    checkmarkColor = Color.White,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                // Names the checkbox for TalkBack: "Gym, checkbox, not checked".
                modifier = Modifier.semantics { contentDescription = habit.name },
            )
        },
    )
}

@Composable
private fun ConfirmRemoveDialog(
    habits: List<Habit>,
    onConfirm: (keepStats: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var keepStats by rememberSaveable { mutableStateOf(true) }
    val count = habits.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Remove this habit?" else "Remove $count habits?") },
        text = {
            // Scrolls when many habits are selected, so the switch is never pushed off screen.
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(habits.joinToString("\n") { "• ${it.name}" })
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .toggleable(
                            value = keepStats,
                            role = Role.Switch,
                            onValueChange = { keepStats = it },
                        ),
                ) {
                    Text(
                        text = "Keep stats",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    // The row handles the toggle, so the switch itself is not a second target.
                    Switch(checked = keepStats, onCheckedChange = null)
                }
                val history = if (count == 1) "Its history" else "Their history"
                Text(
                    text = if (keepStats) {
                        "$history stays in Statistics, under Deleted."
                    } else {
                        "$history is deleted for good. This can't be undone."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (keepStats) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(keepStats) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
