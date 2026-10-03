package com.reevan.reevzhabitz.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.common.HabitCard
import com.reevan.reevzhabitz.ui.common.HabitCardDivider
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.current

/**
 * Today's habits as a gapless stack of cards. To-do habits sit on top; ticking one crosses it out
 * and it slides down to join the done ones. Unticking asks first, since it is the less likely and
 * more costly tap.
 *
 * [state] is null until the first read, and nothing is drawn until then.
 */
@Composable
fun HomeScreen(
    state: HomeUiState?,
    onMarkDone: (habitId: Long) -> Unit,
    onMarkNotDone: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == null) return
    if (state.habits.isEmpty()) {
        EmptyHome(modifier)
        return
    }

    var confirmingUndo by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(modifier.fillMaxSize()) {
        items(state.habits, key = { it.habit.id }) { item ->
            // Divider inside the item, so it travels with its card when the card moves.
            Column(Modifier.animateItem()) {
                HabitRow(
                    item = item,
                    onCheck = {
                        if (item.done) confirmingUndo = item.habit.id else onMarkDone(item.habit.id)
                    },
                )
                HabitCardDivider()
            }
        }
    }

    val undoing = confirmingUndo?.let { id -> state.habits.firstOrNull { it.habit.id == id } }
    if (undoing != null) {
        AlertDialog(
            onDismissRequest = { confirmingUndo = null },
            title = { Text("Mark as not done?") },
            text = { Text("“${undoing.habit.name}” will go back to not done for today.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMarkNotDone(undoing.habit.id)
                        confirmingUndo = null
                    },
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { confirmingUndo = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HabitRow(item: HabitOnDay, onCheck: () -> Unit) {
    val color = HabitColors.forKey(item.habit.colorKey)
    HabitCard(
        name = item.habit.name,
        color = color,
        icon = HabitIcons.forKey(item.habit.iconKey),
        crossedOut = item.done,
        trailing = {
            // Only the checkbox ticks; the card body is not a target.
            Checkbox(
                checked = item.done,
                onCheckedChange = { onCheck() },
                colors = CheckboxDefaults.colors(
                    checkedColor = color.current,
                    checkmarkColor = Color.White,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    )
}

@Composable
private fun EmptyHome(modifier: Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Text(
            text = "No habits for today.\nAdd one from the menu.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The header's sort button. Its icon shows the current order; each tap moves to the next. */
@Composable
fun SortButton(sort: HomeSort, onClick: () -> Unit) {
    val icon = when (sort) {
        HomeSort.ALPHABETICAL -> R.drawable.ic_sort_alphabetical
        HomeSort.NEWEST_FIRST -> R.drawable.ic_sort_newest
        HomeSort.OLDEST_FIRST -> R.drawable.ic_sort_oldest
    }
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(icon),
            contentDescription = "Sorted ${sort.label}. Change sort",
        )
    }
}
