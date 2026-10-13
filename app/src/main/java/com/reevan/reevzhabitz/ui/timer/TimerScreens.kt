package com.reevan.reevzhabitz.ui.timer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.timer.TimerController
import com.reevan.reevzhabitz.timer.TimerDigits
import com.reevan.reevzhabitz.timer.TimerState
import com.reevan.reevzhabitz.timer.formatTimer
import com.reevan.reevzhabitz.timer.fractionLeft
import com.reevan.reevzhabitz.timer.isUnderway
import com.reevan.reevzhabitz.timer.left
import com.reevan.reevzhabitz.timer.preset

/*
 * The Timer, laid out like the Clock app's. Saved timers are cards on the Timer screen; tapping
 * one opens its page (Timer > 25:00), where play starts it. + opens the number pad (Timer > New)
 * for a new time, which is saved as a card and started. One timer runs at a time, in
 * TimerController, so it carries on after these screens are left.
 */

/**
 * Menu > Timer: the saved timers as cards, shortest first, and + for a new one. The one going
 * shows its time left live. With none saved yet the number pad shows straight away, as in the
 * Clock app, and starting opens the new timer's page via [onOpen].
 */
@Composable
fun TimerListScreen(
    onOpen: (duration: Long) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer = rememberTimer()
    val saved by timer.saved.collectAsStateWithLifecycle()
    val state by timer.state.collectAsStateWithLifecycle()
    val now by rememberTimerNow(state, timer)
    if (saved.isEmpty()) {
        NewTimerScreen(onStarted = onOpen, modifier = modifier)
        return
    }
    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            // Room at the bottom so the last cards scroll clear of the + button.
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 128.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(saved, key = { it }) { duration ->
                TimerCard(
                    duration = duration,
                    going = state.takeIf { it.preset == duration },
                    now = now,
                    onClick = { onOpen(duration) },
                )
            }
        }
        FloatingActionButton(
            onClick = onNew,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(72.dp),
        ) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = "New timer")
        }
    }
}

/** A saved timer's card. While it is [going] it shows the time left and how it stands. */
@Composable
private fun TimerCard(duration: Long, going: TimerState?, now: Long, onClick: () -> Unit) {
    val accent = when (going) {
        null -> MaterialTheme.colorScheme.onSurface
        is TimerState.Ringing -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (going != null) BorderStroke(2.dp, accent) else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = formatTimer(going?.left(now) ?: duration),
                style = MaterialTheme.typography.headlineLarge,
                color = accent,
                maxLines = 1,
            )
            val status = when (going) {
                is TimerState.Running -> "Running"
                is TimerState.Paused -> "Paused"
                is TimerState.Ringing -> "Time's up"
                else -> null
            }
            if (status != null) {
                Text(status, style = MaterialTheme.typography.labelLarge, color = accent)
            }
        }
    }
}

/**
 * Timer > New: type a time and press play. It is saved as a card and started, and [onStarted]
 * moves on to its page.
 */
@Composable
fun NewTimerScreen(onStarted: (duration: Long) -> Unit, modifier: Modifier = Modifier) {
    val timer = rememberTimer()
    var typed by rememberSaveable { mutableStateOf("") }
    val digits = TimerDigits(typed)
    val start = rememberTimerStarter(timer, onStarted)
    TimerKeypad(
        digits = digits,
        onKey = { typed = digits.type(it).digits },
        onBackspace = { typed = digits.backspace().digits },
        onClear = { typed = "" },
        onStart = { start(digits.duration) },
        modifier = modifier,
    )
}

/**
 * Timer > 25:00: one saved timer, full screen. Ready, it shows its full ring and time, with play
 * and a bin. Once started it counts down with pause/resume, +1:00 and Reset, then rings until
 * Stop; Reset and Stop bring it back to ready.
 */
@Composable
fun SavedTimerScreen(duration: Long, onDeleted: () -> Unit, modifier: Modifier = Modifier) {
    val timer = rememberTimer()
    val state by timer.state.collectAsStateWithLifecycle()
    val start = rememberTimerStarter(timer, onStarted = {})
    val mine = state.takeIf { it.preset == duration }
    if (mine != null) {
        TimerCountdown(state = mine, timer = timer, modifier = modifier)
    } else {
        TimerPage(
            fraction = 1f,
            ringColor = MaterialTheme.colorScheme.primary,
            modifier = modifier,
            leftSlot = {
                IconButton(
                    onClick = {
                        timer.delete(duration)
                        onDeleted()
                    },
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "Delete timer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            mainButton = {
                MainButton(icon = R.drawable.ic_play, description = "Start", onClick = { start(duration) })
            },
        ) {
            TimeReading(formatTimer(duration))
        }
    }
}

/** The page of the timer that is going: counting down, paused, or ringing. */
@Composable
private fun TimerCountdown(state: TimerState, timer: TimerController, modifier: Modifier = Modifier) {
    val now by rememberTimerNow(state, timer)
    val ringing = state is TimerState.Ringing
    val paused = state is TimerState.Paused
    val pulse = rememberPulse()
    TimerPage(
        fraction = if (ringing) 1f else state.fractionLeft(now),
        ringColor = if (ringing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ringAlpha = if (ringing) pulse else 1f,
        modifier = modifier,
        leftSlot = {
            if (!ringing) {
                TextButton(onClick = timer::reset, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Reset", style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        mainButton = {
            when {
                ringing -> MainButton(R.drawable.ic_stop, "Stop", timer::reset, alarm = true)
                paused -> MainButton(R.drawable.ic_play, "Resume", timer::resume)
                else -> MainButton(R.drawable.ic_pause, "Pause", timer::pause)
            }
        },
    ) {
        if (ringing) {
            Text(
                text = "Time's up",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        TimeReading(formatTimer(state.left(now)), Modifier.alpha(if (paused) pulse else 1f))
        TextButton(
            onClick = timer::addMinute,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "Add one minute" },
        ) {
            Text("+1:00", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TimeReading(reading: String, modifier: Modifier = Modifier) {
    Text(
        text = reading,
        style = if (reading.length > 5) {
            MaterialTheme.typography.displayMedium
        } else {
            MaterialTheme.typography.displayLarge
        },
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

/**
 * Starting a timer from any screen. One timer runs at a time, so if another is counting down or
 * paused it asks before replacing it (a ringing one has finished, so it is simply silenced). It
 * also asks once for notifications; the timer runs whatever the answer.
 */
@Composable
private fun rememberTimerStarter(
    timer: TimerController,
    onStarted: (duration: Long) -> Unit,
): (duration: Long) -> Unit {
    val context = LocalContext.current
    val currentOnStarted by rememberUpdatedState(onStarted)
    val askForNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) timer.refreshNotification() }
    val state by timer.state.collectAsStateWithLifecycle()
    var replacing by rememberSaveable { mutableStateOf<Long?>(null) }

    val go: (Long) -> Unit = { duration ->
        if (needsNotificationPermission(context)) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        timer.start(duration)
        currentOnStarted(duration)
    }

    // If the other timer stops on its own while this is asked (it ran out), the question is moot.
    LaunchedEffect(replacing, state.isUnderway) {
        if (!state.isUnderway) replacing = null
    }
    val next = replacing
    val preset = state.preset
    if (next != null && state.isUnderway && preset != null) {
        val how = if (state is TimerState.Paused) "is paused" else "is running"
        AlertDialog(
            onDismissRequest = { replacing = null },
            title = { Text("Replace the running timer?") },
            text = {
                Text(
                    "The ${formatTimer(preset)} timer $how with " +
                        "${formatTimer(state.left(timer.now()))} left. " +
                        "Start ${formatTimer(next)} instead?",
                )
            },
            confirmButton = {
                TextButton(onClick = { replacing = null; go(next) }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { replacing = null }) { Text("Cancel") }
            },
        )
    }

    return { duration ->
        if (duration > 0) {
            if (state.isUnderway) replacing = duration else go(duration)
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
