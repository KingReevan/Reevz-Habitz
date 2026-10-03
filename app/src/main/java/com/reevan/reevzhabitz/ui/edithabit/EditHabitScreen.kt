package com.reevan.reevzhabitz.ui.edithabit

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.ui.habitform.HabitDetailsFields
import com.reevan.reevzhabitz.ui.habitform.HabitEdits
import com.reevan.reevzhabitz.ui.habitform.HabitFormScaffold
import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.navigation.InterceptLeaving
import com.reevan.reevzhabitz.ui.navigation.LeaveGuard

/**
 * The editor for one habit: name, description, colour and icon, then Save — which stores the
 * changes and returns to the list via [onDone].
 *
 * Leaving by any route — system back, the header arrow, or a breadcrumb — goes at once when
 * nothing has changed. With unsaved changes it is held up by [leaveGuard] to ask "Discard
 * changes?", and Discard then goes wherever the user was heading, through [onLeave].
 */
@Composable
fun EditHabitScreen(
    habitId: Long,
    leaveGuard: LeaveGuard,
    onLeave: (target: List<Destination>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditHabitViewModel = viewModel(factory = EditHabitViewModel.Factory),
) {
    // The stored habit, re-read on every entry (and after a rotation) as the baseline for "changed".
    var original by remember { mutableStateOf<Habit?>(null) }

    // The edits. Saved so a rotation keeps them; filled from the stored habit once only.
    val name = rememberTextFieldState()
    var description by rememberSaveable { mutableStateOf("") }
    var colorKey by rememberSaveable { mutableStateOf("") }
    var iconKey by rememberSaveable { mutableStateOf("") }
    var filled by rememberSaveable { mutableStateOf(false) }

    // Where the user tried to go with unsaved changes, while "Discard changes?" is up. Saved as
    // routes, so after a rotation Discard still lands on the screen they chose.
    var pendingLeave by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(habitId) {
        val habit = viewModel.load(habitId)
        if (habit == null) {
            // Removed while we were away; there is nothing to edit.
            onDone()
            return@LaunchedEffect
        }
        if (!filled) {
            name.setTextAndPlaceCursorAtEnd(habit.name)
            description = habit.description
            colorKey = habit.colorKey
            iconKey = habit.iconKey
            filled = true
        }
        original = habit
    }

    val stored = original ?: return
    val edits = HabitEdits(
        name = name.text.toString(),
        description = description,
        colorKey = colorKey,
        iconKey = iconKey,
    )
    val changed = edits.changes(stored)

    leaveGuard.InterceptLeaving(enabled = changed && !saving) { target ->
        pendingLeave = target.map { it.route }
    }

    HabitFormScaffold(
        actionLabel = "Save",
        actionEnabled = changed && edits.isComplete && !saving,
        onAction = {
            // Checked at tap time, not just via `enabled`, which only updates on the next frame.
            if (!saving) {
                saving = true
                viewModel.save(stored, edits, onDone)
            }
        },
        modifier = modifier,
    ) {
        HabitDetailsFields(
            name = name,
            description = description,
            onDescriptionChange = { description = it },
            colorKey = colorKey,
            onColorChange = { colorKey = it },
            iconKey = iconKey,
            onIconChange = { iconKey = it },
        )
    }

    val leaving = pendingLeave
    if (leaving != null) {
        AlertDialog(
            onDismissRequest = { pendingLeave = null },
            title = { Text("Discard changes?") },
            text = { Text("Your changes to “${stored.name}” will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingLeave = null
                        onLeave(leaving.map(Destination::fromRoute))
                    },
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { pendingLeave = null }) { Text("Keep editing") }
            },
        )
    }
}
