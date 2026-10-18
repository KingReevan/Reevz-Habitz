package com.reevan.reevzhabitz.ui.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
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
 * section. Tapping one opens its calendar. Every card is as tall as the tallest, so a one-line
 * description and a two-line one look alike.
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

    SameHeightCards(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        state.active.forEach { habit ->
            key(habit.id) { StatsHabitCard(habit, onOpen) }
        }
        if (state.deleted.isNotEmpty()) {
            Text(
                text = "Deleted",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            )
            HabitCardDivider()
            state.deleted.forEach { habit ->
                key(habit.id) { StatsHabitCard(habit, onOpen) }
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
        modifier = Modifier.layoutId(CARD),
    )
    HabitCardDivider()
}

/** Tags the children of [SameHeightCards] that are cards. */
private const val CARD = "card"

/**
 * Stacks its children top to bottom, like a Column, but makes every child tagged
 * `Modifier.layoutId(CARD)` as tall as the tallest card; the section heading and dividers keep
 * their own heights. Not lazy: the tallest card has to be known before any is placed, and the list
 * is short (one card per habit).
 */
@Composable
private fun SameHeightCards(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val each = Constraints(minWidth = width, maxWidth = width)
        val tallest = measurables.filter { it.layoutId == CARD }
            .maxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0
        val placeables = measurables.map {
            it.measure(if (it.layoutId == CARD) each.copy(minHeight = tallest, maxHeight = tallest) else each)
        }
        // At least the screen's height when scrolled, so a short list still starts at the top.
        val height = maxOf(placeables.sumOf { it.height }, constraints.minHeight)
        layout(width, height) {
            var y = 0
            placeables.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}
