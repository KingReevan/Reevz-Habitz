package com.reevan.reevzhabitz.ui.addhabit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.common.currentLocale
import com.reevan.reevzhabitz.ui.common.firstDayOfWeek
import com.reevan.reevzhabitz.ui.habitform.FormSectionTitle
import com.reevan.reevzhabitz.util.formatWeekdays
import com.reevan.reevzhabitz.util.weekStartingOn
import java.time.DayOfWeek
import java.time.format.TextStyle

/** What the Advanced options button reads as to TalkBack, and how tests find it. */
const val ADVANCED_OPTIONS = "Advanced options"

/**
 * Add Habit's Advanced options page (`Home > Menu > Add Habit > Advanced`): settings most habits
 * never need, kept off the form so it stays short. For now, the weekdays the habit is due on;
 * later per-habit options belong here too.
 *
 * It edits the Add Habit form's own state, so going back finds the form exactly as it was left.
 */
@Composable
fun AdvancedOptionsPage(
    days: Set<DayOfWeek>,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekStart = firstDayOfWeek()
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormSectionTitle(
                title = "Repeat on",
                detail = formatWeekdays(days, weekStart, currentLocale()),
            )
            WeekdayPicker(days = days, weekStart = weekStart, onDaysChange = onDaysChange)
            Text(
                text = when (days.size) {
                    0 -> "Pick at least one day."
                    7 -> "Shows on Home every day. Untick the days to skip."
                    else -> "Shows on Home only on these days. Other days don't count in its " +
                        "statistics."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (days.isEmpty()) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = "The days can't be changed once the habit is created.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The week as seven round toggles, in the phone's week order (as in the Statistics calendar). A
 * chosen day is filled; TalkBack reads each by its full name.
 */
@Composable
private fun WeekdayPicker(
    days: Set<DayOfWeek>,
    weekStart: DayOfWeek,
    onDaysChange: (Set<DayOfWeek>) -> Unit,
) {
    val locale = currentLocale()
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        weekStartingOn(weekStart).forEach { day ->
            val chosen = day in days
            val name = day.getDisplayName(TextStyle.FULL, locale)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .then(
                        if (chosen) {
                            Modifier.background(colors.primary)
                        } else {
                            Modifier.border(1.dp, colors.outline, CircleShape)
                        },
                    )
                    .toggleable(
                        value = chosen,
                        role = Role.Checkbox,
                        onValueChange = { onDaysChange(if (it) days + day else days - day) },
                    )
                    .semantics { contentDescription = name },
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (chosen) colors.onPrimary else colors.onSurface,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

/**
 * The small button at the left of Add Habit's bottom bar that opens [AdvancedOptionsPage]. Just
 * an icon while the habit is due every day; once days are picked it also shows them, e.g.
 * "Mon, Fri", so a custom schedule is never hidden.
 */
@Composable
fun AdvancedOptionsButton(days: Set<DayOfWeek>, onClick: () -> Unit) {
    val summary = if (days.size == 7) null else formatWeekdays(days, firstDayOfWeek(), currentLocale())
    val color = when {
        days.isEmpty() -> MaterialTheme.colorScheme.error
        summary != null -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Icon(painterResource(R.drawable.ic_tune), contentDescription = ADVANCED_OPTIONS)
        if (summary != null) {
            Spacer(Modifier.width(8.dp))
            Text(summary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
