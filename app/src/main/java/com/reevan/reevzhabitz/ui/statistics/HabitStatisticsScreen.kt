package com.reevan.reevzhabitz.ui.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.HabitzTheme
import com.reevan.reevzhabitz.ui.theme.current
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * One habit's statistics: a thin collapsible strip with its numbers, then a month calendar with
 * every day as a circle — green done, red missed, grey future (or after deletion), and a yellow
 * ring on today. Opens on the current month, or the deletion month for a deleted habit; pages one
 * month at a time with the year always shown.
 */
@Composable
fun HabitStatisticsScreen(
    habitId: Long,
    onGone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory),
) {
    // Wrapped so "still loading" (null) is distinct from "loaded, and the habit is gone".
    val flow = remember(habitId) { viewModel.habitStats(habitId).map(::Loaded) }
    val loaded by flow.collectAsStateWithLifecycle(initialValue = null)
    val state = (loaded ?: return).state
    if (state == null) {
        // Cleared from Settings while open; nothing left to show.
        LaunchedEffect(Unit) { onGone() }
        return
    }
    val history = state.history

    // The month on show, as a count of months since year 0 so it fits in a Bundle. Unset until the
    // habit has loaded, then opened on the habit's opening month.
    var shownMonthIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val shownMonth = shownMonthIndex?.let(::monthFromIndex) ?: history.openingMonth
    val month = shownMonth.coerceIn(history.firstMonth, history.lastMonth)

    Column(modifier.fillMaxSize()) {
        StatsStrip(habit = state.habit, summary = history.summary)
        HorizontalDivider(color = HabitzTheme.colors.divider)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            MonthHeader(
                month = month,
                canGoBack = month > history.firstMonth,
                canGoForward = month < history.lastMonth,
                onBack = { shownMonthIndex = month.minusMonths(1).toIndex() },
                onForward = { shownMonthIndex = month.plusMonths(1).toIndex() },
            )
            MonthCalendar(month = month, history = history)
        }
    }
}

private class Loaded(val state: HabitStatsState?)

/** The phone's locale, read so the calendar recomposes if the language or region changes. */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

private fun YearMonth.toIndex(): Int = year * 12 + (monthValue - 1)

private fun monthFromIndex(index: Int): YearMonth = YearMonth.of(index / 12, index % 12 + 1)

private fun YearMonth.coerceIn(min: YearMonth, max: YearMonth): YearMonth = when {
    this < min -> min
    this > max -> max
    else -> this
}

/**
 * The thin strip under the header: the habit's icon and name, and a chevron. Collapsed by default;
 * tapping it reveals the three numbers.
 */
@Composable
private fun StatsStrip(habit: Habit, summary: StatsSummary) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val color = HabitColors.forKey(habit.colorKey).current
    val chevronTurn by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Hide stats" else "Show stats",
                ) { expanded = !expanded }
                .padding(horizontal = 16.dp),
        ) {
            Icon(
                painter = painterResource(HabitIcons.forKey(habit.iconKey).drawable),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = habit.name,
                color = color,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronTurn),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            ) {
                StatLine("Days done", "${summary.done}/${summary.due}")
                StatLine("Current streak", days(summary.currentStreak))
                StatLine("Longest streak", days(summary.longestStreak))
            }
        }
    }
}

/**
 * Black or white, whichever contrasts more with [background]. The status colours range from deep
 * (Light's #2E7D32 green) to pale (Tokyo Night's #F7768E pink), so no single text colour reads on
 * all of them.
 */
private fun readableOn(background: Color): Color {
    val l = background.luminance()
    val onWhite = 1.05f / (l + 0.05f)
    val onBlack = (l + 0.05f) / 0.05f
    return if (onBlack > onWhite) Color.Black else Color.White
}

private fun days(count: Int) = if (count == 1) "1 day" else "$count days"

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "October 2026" between previous / next arrows; an arrow is disabled at the edge of the range. */
@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = onBack, enabled = canGoBack) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month")
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale())),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onForward, enabled = canGoForward) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next month")
        }
    }
}

/** Weekday initials, then the month's days as circles, seven to a row. */
@Composable
private fun MonthCalendar(month: YearMonth, history: HabitHistory) {
    val locale = currentLocale()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val cells = remember(month, firstDayOfWeek) { monthGrid(month, firstDayOfWeek) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { i ->
                Text(
                    text = firstDayOfWeek.plus(i.toLong()).getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp),
                    ) {
                        if (day != null) DayCircle(day, history)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCircle(day: LocalDate, history: HabitHistory) {
    val colors = HabitzTheme.colors
    val status = history.statusOn(day)
    val fill = when (status) {
        DayStatus.DONE -> colors.done
        DayStatus.MISSED -> colors.missed
        DayStatus.FUTURE, DayStatus.AFTER_DELETION -> colors.future
    }
    val number = when (status) {
        DayStatus.FUTURE, DayStatus.AFTER_DELETION -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> readableOn(fill)
    }
    val isToday = day == history.today
    val statusLabel = when (status) {
        DayStatus.DONE -> "done"
        DayStatus.MISSED -> "missed"
        DayStatus.FUTURE -> "upcoming"
        DayStatus.AFTER_DELETION -> "after deletion"
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .then(
                // Today: a yellow ring around the circle, with a gap so it reads as a ring.
                if (isToday) Modifier.border(3.dp, colors.todayRing, CircleShape).padding(5.dp)
                else Modifier.padding(2.dp),
            )
            .background(fill, CircleShape)
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "${day.dayOfMonth}, $statusLabel" + if (isToday) ", today" else ""
            },
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = number,
        )
    }
}
