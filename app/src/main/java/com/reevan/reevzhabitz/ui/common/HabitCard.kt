package com.reevan.reevzhabitz.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.ui.theme.HabitColor
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcon
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.HabitzTheme
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme
import com.reevan.reevzhabitz.ui.theme.current

private val EdgeWidth = 52.dp
private val EdgeIconSize = 26.dp
private val MinCardHeight = 56.dp

/**
 * One habit as a full-width card, shared by Home, Remove Habit, Edit Habit and Statistics.
 *
 * A coloured left edge carries the habit's icon in pure white; the name follows in the same colour
 * and wraps rather than running under [trailing]. [description] is shown only where a screen
 * asks for it (Remove, Edit, Statistics). [crossedOut] strikes the name through, for a habit done
 * today. Cards are stacked with no gaps — put a [HabitCardDivider] between them.
 */
@Composable
fun HabitCard(
    name: String,
    color: HabitColor,
    icon: HabitIcon,
    modifier: Modifier = Modifier,
    description: String? = null,
    crossedOut: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val habitColor = color.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            // Lets the edge stretch to the card's full height when a long name wraps.
            .height(IntrinsicSize.Min)
            .heightIn(min = MinCardHeight)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(EdgeWidth)
                .fillMaxHeight()
                .background(habitColor),
        ) {
            Icon(
                painter = painterResource(icon.drawable),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(EdgeIconSize),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = if (trailing == null) 16.dp else 4.dp)
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = name,
                color = habitColor,
                style = MaterialTheme.typography.titleMedium,
                textDecoration = if (crossedOut) TextDecoration.LineThrough else null,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        trailing?.invoke()
    }
}

/** The thin grey line between stacked habit cards. */
@Composable
fun HabitCardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = HabitzTheme.colors.divider)
}

@Composable
private fun SampleStack() {
    Surface {
        Column {
            HabitCard(
                name = "Drink Water",
                color = HabitColors.forKey("sky"),
                icon = HabitIcons.forKey("water"),
                trailing = { Checkbox(checked = false, onCheckedChange = {}) },
            )
            HabitCardDivider()
            HabitCard(
                name = "Read Twenty Pages Of A Book Before Going To Sleep Every Night",
                color = HabitColors.forKey("violet"),
                icon = HabitIcons.forKey("book"),
                description = "Fiction or non-fiction, anything but the phone.",
                trailing = { Checkbox(checked = false, onCheckedChange = {}) },
            )
            HabitCardDivider()
            HabitCard(
                name = "Gym",
                color = HabitColors.forKey("coral"),
                icon = HabitIcons.forKey("dumbbell"),
                crossedOut = true,
                trailing = { Checkbox(checked = true, onCheckedChange = {}) },
            )
            HabitCardDivider()
        }
    }
}

@Preview(name = "Light")
@Composable
private fun HabitCardLightPreview() {
    ReevzHabitzTheme(ThemeMode.LIGHT) { SampleStack() }
}

@Preview(name = "Dark")
@Composable
private fun HabitCardDarkPreview() {
    ReevzHabitzTheme(ThemeMode.DARK) { SampleStack() }
}

@Preview(name = "VS Code Dark")
@Composable
private fun HabitCardVsCodePreview() {
    ReevzHabitzTheme(ThemeMode.VSCODE_DARK) { SampleStack() }
}

@Preview(name = "Tokyo Night")
@Composable
private fun HabitCardTokyoPreview() {
    ReevzHabitzTheme(ThemeMode.TOKYO_NIGHT) { SampleStack() }
}
