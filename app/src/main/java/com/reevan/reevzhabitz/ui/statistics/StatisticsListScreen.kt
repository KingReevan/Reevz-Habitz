package com.reevan.reevzhabitz.ui.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.ui.common.HabitCard
import com.reevan.reevzhabitz.ui.common.HabitCardDivider
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons

/**
 * Statistics: habits as cards with descriptions — active ones first, then a separate Deleted
 * section. Tapping one opens its calendar.
 */
@Composable
fun StatisticsListScreen(
    onOpen: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory),
) {
    val list by viewModel.list.collectAsStateWithLifecycle()
    val state = list ?: return
    if (state.active.isEmpty() && state.deleted.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = "No stats yet.\nHabits appear here once they start.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier.fillMaxSize()) {
        items(state.active, key = { it.id }) { habit ->
            StatsHabitCard(habit, onOpen)
        }
        if (state.deleted.isNotEmpty()) {
            item(key = "deleted-header") {
                Text(
                    text = "Deleted",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
                HabitCardDivider()
            }
            items(state.deleted, key = { it.id }) { habit ->
                StatsHabitCard(habit, onOpen)
            }
        }
    }
}

@Composable
private fun StatsHabitCard(habit: Habit, onOpen: (Long) -> Unit) {
    HabitCard(
        name = habit.name,
        color = HabitColors.forKey(habit.colorKey),
        icon = HabitIcons.forKey(habit.iconKey),
        description = habit.description,
        onClick = { onOpen(habit.id) },
    )
    HabitCardDivider()
}
