package com.reevan.reevzhabitz.ui.habitorder

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.ui.common.HabitCard
import com.reevan.reevzhabitz.ui.common.HabitCardDivider
import com.reevan.reevzhabitz.ui.common.SameHeightCards
import com.reevan.reevzhabitz.ui.common.rememberSubmission
import com.reevan.reevzhabitz.ui.common.sameHeightCard
import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.navigation.InterceptLeaving
import com.reevan.reevzhabitz.ui.navigation.LeaveGuard
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.HabitzTheme

/** TalkBack's way to move a card, in place of dragging; tests use them too. */
const val MOVE_UP = "Move up"
const val MOVE_DOWN = "Move down"

/**
 * Habit Order: every active habit with its description, numbered on the right in Home's order.
 * Hold a card and drag it to move it; Save stores the order for Home — and only Home — then
 * returns to the Menu via [onDone].
 *
 * Leaving with an unsaved order asks "Discard changes?" first, like the Edit Habit editor: held up
 * by [leaveGuard], then sent on to wherever the user was heading through [onLeave].
 */
@Composable
fun HabitOrderScreen(
    leaveGuard: LeaveGuard,
    onLeave: (target: List<Destination>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitOrderViewModel = viewModel(factory = HabitOrderViewModel.Factory),
) {
    val saving = rememberSubmission(viewModel.submissions, onFinished = onDone)
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    // The unsaved order, top first; null until something is moved. Saved across a rotation.
    var edited by rememberSaveable { mutableStateOf<List<Long>?>(null) }
    // Where the user tried to go with an unsaved order, while "Discard changes?" is up.
    var pendingLeave by rememberSaveable { mutableStateOf<List<String>?>(null) }

    val all = habits ?: return
    val stored = all.map { it.id }
    val order = shownOrder(edited, stored)
    val changed = order != stored

    leaveGuard.InterceptLeaving(enabled = changed && !saving.inProgress) { target ->
        pendingLeave = target.map { it.route }
    }

    if (all.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = "No habits to order.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    Column(modifier.fillMaxSize()) {
        ReorderableHabits(
            habits = all,
            order = order,
            onOrderChange = { edited = it },
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Hold a habit, then drag it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { saving.start()?.let { viewModel.save(it, order) } },
                enabled = changed && !saving.inProgress,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Save")
            }
        }
    }

    val leaving = pendingLeave
    if (leaving != null) {
        AlertDialog(
            onDismissRequest = { pendingLeave = null },
            title = { Text("Discard changes?") },
            text = { Text("Your new order will be lost.") },
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

/**
 * The cards, every one as tall as the tallest, in [order]. A long press lifts a card; it then
 * follows the finger, swapping with each neighbour it passes ([dragStep]), and the list scrolls
 * when the finger nears its top or bottom edge. Letting go hands the new order to [onOrderChange].
 *
 * The gesture is caught on the unmoving viewport rather than on the card: a card's own
 * coordinates shift every time it swaps or the list scrolls, which would throw its drag deltas
 * off. Equal heights make every slot the same size, so a touch's slot is just its height divided
 * by one slot.
 */
@Composable
private fun ReorderableHabits(
    habits: List<Habit>,
    order: List<Long>,
    onOrderChange: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val byId = remember(habits) { habits.associateBy { it.id } }
    val scroll = rememberScrollState()
    val haptics = LocalHapticFeedback.current
    val currentOrder by rememberUpdatedState(order)
    val currentOnOrderChange by rememberUpdatedState(onOrderChange)

    val density = LocalDensity.current
    val dividerPx = with(density) { 1.dp.toPx() }
    var cardPx by remember { mutableIntStateOf(0) }
    var viewportPx by remember { mutableIntStateOf(0) }
    // The drag in progress: the card lifted, the order as it stands mid-drag, the card's offset
    // from its own slot, and the finger's height in the viewport.
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOrder by remember { mutableStateOf<List<Long>?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var fingerY by remember { mutableFloatStateOf(0f) }

    // Moves the lifted card by [dy] and swaps it past any neighbour it now covers.
    fun follow(dy: Float) {
        val id = draggedId ?: return
        val list = dragOrder ?: return
        val step = dragStep(list, id, offset + dy, cardPx + dividerPx)
        if (step.order != list) {
            dragOrder = step.order
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
        offset = step.offset
    }

    fun drop() {
        dragOrder?.let { currentOnOrderChange(it) }
        draggedId = null
        dragOrder = null
        offset = 0f
    }

    // Scrolls the list while the finger is held near an edge, faster the closer it gets. The
    // lifted card stays with the finger, so it moves through the list as it scrolls past.
    LaunchedEffect(draggedId) {
        if (draggedId == null) return@LaunchedEffect
        val edge = with(density) { 64.dp.toPx() }
        val fastest = with(density) { 12.dp.toPx() }
        while (draggedId != null) {
            withFrameNanos { }
            val speed = when {
                fingerY < edge -> -fastest * ((edge - fingerY) / edge).coerceAtMost(1f)
                fingerY > viewportPx - edge ->
                    fastest * ((fingerY - (viewportPx - edge)) / edge).coerceAtMost(1f)
                else -> 0f
            }
            if (speed != 0f) follow(scroll.scrollBy(speed))
        }
    }

    Box(
        modifier
            .onSizeChanged { viewportPx = it.height }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { at ->
                        val slot = cardPx + dividerPx
                        val id = currentOrder.getOrNull(((at.y + scroll.value) / slot).toInt())
                        if (cardPx > 0 && id != null) {
                            dragOrder = currentOrder
                            draggedId = id
                            offset = 0f
                            fingerY = at.y
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        fingerY += amount.y
                        follow(amount.y)
                    },
                    onDragEnd = { drop() },
                    onDragCancel = { drop() },
                )
            },
    ) {
        val shown = dragOrder ?: order
        SameHeightCards(Modifier.verticalScroll(scroll, enabled = draggedId == null)) {
            shown.forEachIndexed { index, id ->
                val habit = byId[id] ?: return@forEachIndexed
                key(id) {
                    val lifted = id == draggedId
                    PositionedHabitCard(
                        habit = habit,
                        position = index + 1,
                        modifier = Modifier
                            .sameHeightCard()
                            .onSizeChanged { cardPx = it.height }
                            .zIndex(if (lifted) 1f else 0f)
                            .graphicsLayer {
                                if (lifted) {
                                    translationY = offset
                                    shadowElevation = 8.dp.toPx()
                                }
                            }
                            .background(
                                if (lifted) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    MaterialTheme.colorScheme.background
                                },
                            )
                            .semantics(mergeDescendants = true) {
                                customActions = listOfNotNull(
                                    CustomAccessibilityAction(MOVE_UP) {
                                        currentOnOrderChange(shown.moved(index, index - 1))
                                        true
                                    }.takeIf { index > 0 },
                                    CustomAccessibilityAction(MOVE_DOWN) {
                                        currentOnOrderChange(shown.moved(index, index + 1))
                                        true
                                    }.takeIf { index < shown.lastIndex },
                                )
                            },
                    )
                    HabitCardDivider()
                }
            }
        }
    }
}

/** A habit card with its description and, at the right end, its place in the order. */
@Composable
private fun PositionedHabitCard(habit: Habit, position: Int, modifier: Modifier = Modifier) {
    HabitCard(
        name = habit.name,
        color = HabitColors.forKey(habit.colorKey),
        icon = HabitIcons.forKey(habit.iconKey),
        description = habit.description,
        modifier = modifier,
        trailing = {
            Row(Modifier.fillMaxHeight()) {
                VerticalDivider(color = HabitzTheme.colors.divider)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
