package com.reevan.reevzhabitz.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.timer.TimerController
import com.reevan.reevzhabitz.timer.TimerDigits
import com.reevan.reevzhabitz.timer.TimerState

/*
 * Pieces shared by the Timer screens (TimerScreens.kt): the number pad, the ring page and its
 * big round button.
 */

private val KeySize = 76.dp
private val KeyGap = 20.dp
private val RingWidth = 10.dp
internal val SideSlot = 96.dp

@Composable
internal fun rememberTimer(): TimerController {
    val context = LocalContext.current
    return remember(context) { TimerController.get(context) }
}

/**
 * The since-boot clock, re-read every frame while [state] is counting (for a smooth ring and live
 * times). Reaching zero settles the timer right away, so it rings on time even if the alarm is a
 * moment late. These are "infinite animation" frames, so UI tests (which pause those) can still
 * see the screen go idle; there the alarm settles the timer.
 */
@Composable
internal fun rememberTimerNow(state: TimerState, timer: TimerController): State<Long> =
    produceState(timer.now(), state) {
        while (state is TimerState.Running || state is TimerState.Ringing) {
            withInfiniteAnimationFrameMillis { }
            value = timer.now()
            if (state is TimerState.Running && value >= state.endsAt) timer.settle()
        }
    }

/** Fades between full and faint: a paused time blinks and a ringing ring pulses, as in the Clock app. */
@Composable
internal fun rememberPulse(): Float {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
        label = "pulse",
    )
    return pulse
}

/** The Clock app's number pad: type a time, which fills "hh mm ss" from the right, then play. */
@Composable
internal fun TimerKeypad(
    digits: TimerDigits,
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // fillMaxSize before verticalScroll keeps the viewport height as a minimum, so this stays
    // centred and only scrolls when it doesn't fit (landscape, big fonts).
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        DurationDisplay(digits)
        Column(verticalArrangement = Arrangement.spacedBy(KeyGap)) {
            listOf("123", "456", "789").forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                    row.forEach { key -> DigitKey(key.toString(), onKey) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                DigitKey("00", onKey)
                DigitKey("0", onKey)
                Key(onClick = onBackspace, onLongClick = onClear, description = "Delete") {
                    Icon(
                        painter = painterResource(R.drawable.ic_backspace),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        StartButton(visible = !digits.isEmpty, onClick = onStart)
    }
}

/** Play, shown once a time is typed. Its space is kept while hidden, so the keypad doesn't jump. */
@Composable
private fun StartButton(visible: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(SideSlot)) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            MainButton(icon = R.drawable.ic_play, description = "Start", onClick = onClick)
        }
    }
}

/** "00h 05m 30s", the typed digits in the accent colour and the rest dimmed, as in the Clock app. */
@Composable
private fun DurationDisplay(digits: TimerDigits) {
    val typedColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.onSurfaceVariant
    val all = digits.hours + digits.minutes + digits.seconds
    val firstTyped = all.length - digits.digits.length
    val text = buildAnnotatedString {
        listOf("h", "m", "s").forEachIndexed { pair, unit ->
            if (pair > 0) append(" ")
            val start = pair * 2
            for (i in start until start + 2) {
                withStyle(SpanStyle(color = if (i >= firstTyped) typedColor else emptyColor)) {
                    append(all[i])
                }
            }
            val unitColor = if (start + 1 >= firstTyped) typedColor else emptyColor
            withStyle(SpanStyle(color = unitColor, fontSize = 0.45.em)) { append(unit) }
        }
    }
    val spoken = "${digits.hours.toInt()} hours, ${digits.minutes.toInt()} minutes, " +
        "${digits.seconds.toInt()} seconds"
    Text(
        text = text,
        style = MaterialTheme.typography.displayLarge,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.semantics { contentDescription = spoken },
    )
}

@Composable
private fun DigitKey(key: String, onKey: (String) -> Unit) {
    Key(onClick = { onKey(key) }) {
        Text(
            text = key,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun Key(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(KeySize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .then(
                if (description != null) {
                    Modifier.semantics { contentDescription = description }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

/**
 * A timer's full-screen page: the ring with [ringContent] inside, then a row with [leftSlot],
 * the big [mainButton] centred, and an empty slot on the right to keep it centred.
 */
@Composable
internal fun TimerPage(
    fraction: Float,
    ringColor: Color,
    modifier: Modifier = Modifier,
    ringAlpha: Float = 1f,
    leftSlot: @Composable () -> Unit = {},
    mainButton: @Composable () -> Unit,
    ringContent: @Composable ColumnScope.() -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        TimerRing(fraction = fraction, color = ringColor, ringAlpha = ringAlpha, content = ringContent)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(SideSlot)) {
                leftSlot()
            }
            mainButton()
            Spacer(Modifier.width(SideSlot))
        }
    }
}

/** The big round button: start, pause, resume, or stop the ringing. */
@Composable
internal fun MainButton(icon: Int, description: String, onClick: () -> Unit, alarm: Boolean = false) {
    LargeFloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        containerColor = if (alarm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentColor = if (alarm) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(36.dp),
        )
    }
}

/** A circle that empties clockwise from the top as [fraction] falls from 1 to 0. */
@Composable
private fun TimerRing(
    fraction: Float,
    color: Color,
    ringAlpha: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .widthIn(max = 320.dp)
            .aspectRatio(1f)
            .drawBehind {
                val stroke = RingWidth.toPx()
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    alpha = ringAlpha,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}
