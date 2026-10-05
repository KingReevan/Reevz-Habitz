package com.reevan.reevzhabitz.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.common.HabitCard
import com.reevan.reevzhabitz.ui.common.HabitCheckSection
import com.reevan.reevzhabitz.ui.common.HabitCardDivider
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.HabitzTheme
import com.reevan.reevzhabitz.ui.theme.readableOn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Today's habits as a gapless stack of cards. To-do habits sit on top; ticking one crosses it out
 * and it slides down to join the done ones. Unticking asks first, since it is the less likely and
 * more costly tap. Ticking the last one plays a star, and a green card stays pinned at the bottom
 * for as long as every habit is done.
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
    // Before the early returns, so they keep their place in the composition.
    val completion = remember { DayCompletionWatcher() }
    // Bumped each time the user's tick finishes the day; each bump plays the star once.
    var stars by remember { mutableIntStateOf(0) }
    LaunchedEffect(state?.habits) {
        if (state != null && completion.completedByTick(state.habits)) stars++
    }

    if (state == null) return
    if (state.habits.isEmpty()) {
        EmptyHome(modifier)
        return
    }

    var confirmingUndo by rememberSaveable { mutableStateOf<Long?>(null) }
    val allDone = isDayComplete(state.habits)
    // The pinned card's height, so the list can scroll its last habit clear of it.
    var cardHeight by remember { mutableIntStateOf(0) }
    val listEnd = if (allDone) with(LocalDensity.current) { cardHeight.toDp() } else 0.dp

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = listEnd),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.habits, key = { it.habit.id }) { item ->
                // Divider inside the item, so it travels with its card when the card moves.
                Column(Modifier.animateItem()) {
                    HabitRow(
                        item = item,
                        onCheck = {
                            if (item.done) {
                                confirmingUndo = item.habit.id
                            } else {
                                completion.ticked(item.habit.id)
                                onMarkDone(item.habit.id)
                            }
                        },
                    )
                    HabitCardDivider()
                }
            }
        }

        AnimatedVisibility(
            visible = allDone,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { cardHeight = it.height },
        ) {
            AllDoneCard()
        }

        CelebrationStar(trigger = stars, modifier = Modifier.align(Alignment.Center))
    }

    // Only while that habit is still done: if it stops being done with the prompt open — at
    // midnight the new day starts unticked — the prompt has nothing left to ask, so it closes.
    val undoing = confirmingUndo?.let { id ->
        state.habits.firstOrNull { it.habit.id == id && it.done }
    }
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
    val haptics = LocalHapticFeedback.current
    HabitCard(
        name = item.habit.name,
        color = color,
        icon = HabitIcons.forKey(item.habit.iconKey),
        done = item.done,
        trailing = {
            // Only the check section ticks; the card body is not a target.
            HabitCheckSection(
                checked = item.done,
                onCheckedChange = {
                    // A firm "done" buzz when a habit is ticked. Unticking only opens the prompt.
                    if (!item.done) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onCheck()
                },
                label = item.habit.name,
                // Only ever checked when done, so a checked box is always the done grey.
                checkedColor = HabitzTheme.colors.doneHabit,
            )
        },
    )
}

/**
 * Pinned to the bottom of Home while every habit today is done. TalkBack reads it out when it
 * appears.
 */
@Composable
private fun AllDoneCard() {
    val green = HabitzTheme.colors.done
    Surface(
        color = green,
        contentColor = readableOn(green),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag("allDoneCard"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.habit_star),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Everything is complete. Fantastic!",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

/**
 * The big gold star for finishing the day: it pops in, overshooting a little, then fades out,
 * about a second and a half in all, with a firm buzz on top of the tick's own. Plays once each
 * time [trigger] goes up; 0 means it hasn't been earned yet. It never takes touches.
 */
@Composable
private fun CelebrationStar(trigger: Int, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scale.snapTo(0.3f)
        alpha.snapTo(1f)
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow),
                )
            }
            launch {
                delay(900)
                alpha.animateTo(0f, animationSpec = tween(durationMillis = 600))
            }
        }
    }
    if (alpha.value > 0f) {
        Icon(
            painter = painterResource(R.drawable.habit_star),
            contentDescription = null,
            tint = HabitzTheme.colors.star,
            modifier = modifier
                .size(160.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .testTag("celebrationStar"),
        )
    }
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
