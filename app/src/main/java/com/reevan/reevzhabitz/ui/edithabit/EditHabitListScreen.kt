package com.reevan.reevzhabitz.ui.edithabit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.ui.common.HabitCard
import com.reevan.reevzhabitz.ui.common.HabitCardDivider
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons

/** Edit Habit: the habits as cards with descriptions; tapping one opens its editor. */
@Composable
fun EditHabitListScreen(
    onOpen: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditHabitViewModel = viewModel(factory = EditHabitViewModel.Factory),
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
                text = "No habits to edit.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(all, key = { it.id }) { habit ->
            HabitCard(
                name = habit.name,
                color = HabitColors.forKey(habit.colorKey),
                icon = HabitIcons.forKey(habit.iconKey),
                description = habit.description,
                onClick = { onOpen(habit.id) },
            )
            HabitCardDivider()
        }
    }
}
