package com.reevan.reevzhabitz.ui.habitform

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.theme.HabitColor
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons
import com.reevan.reevzhabitz.ui.theme.current
import com.reevan.reevzhabitz.util.capitalizeWords

/*
 * Form pieces shared by Add Habit and the Edit Habit editor.
 */

/** Every option cell is a full 48dp touch target. */
private val CellSize = 48.dp

/**
 * Habit name, shown with every word's first letter capitalised as it is typed.
 *
 * The capitalisation is display-only ([OutputTransformation]); the stored name gets it from
 * [HabitDraft.toHabit]. Changing the text itself while the keyboard is mid-word — whether in
 * onValueChange or an InputTransformation — knocks the keyboard out of sync and drops keystrokes
 * (seen on the emulator: typing "drink water daily and go for a 5km run" left "Drink Water Daily
 * And Go Fo"). An output transformation never touches the text the keyboard owns, so nothing is
 * lost, and every letter it changes maps one-to-one, so the cursor stays where it is drawn.
 */
@Composable
fun HabitNameField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = state,
        outputTransformation = CapitalizeWords,
        label = { Text("Habit name") },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Draws the text through [capitalizeWords], replacing only the letters it changes. */
private val CapitalizeWords = OutputTransformation {
    val current = asCharSequence().toString()
    val capitalized = capitalizeWords(current)
    for (i in current.indices) {
        if (current[i] != capitalized[i]) replace(i, i + 1, capitalized[i].toString())
    }
}

@Composable
fun HabitDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Description") },
        minLines = 2,
        maxLines = 5,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A form section heading. [detail] sits beside it — the current choice, or a nudge when nothing
 * has been picked yet.
 */
@Composable
fun FormSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    detailIsPrompt: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = if (detailIsPrompt) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

/** The habit colours as round swatches. The chosen one carries a check mark and a ring. */
@Composable
fun HabitColorPicker(
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChoiceGrid(items = HabitColors.all, modifier = modifier) { color ->
        val selected = color.key == selectedKey
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CellSize)
                .clip(CircleShape)
                .selectable(selected = selected, role = Role.RadioButton) { onSelect(color.key) }
                .semantics { contentDescription = color.label },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .then(
                        if (selected) {
                            Modifier.border(
                                BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface),
                                CircleShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(if (selected) 4.dp else 0.dp)
                    .background(color.current, CircleShape),
            ) {
                if (selected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/**
 * The habit icons as tiles. Once a colour is chosen every tile previews white-on-that-colour,
 * exactly as the card edge will look; before that they sit on a neutral panel.
 */
@Composable
fun HabitIconPicker(
    selectedKey: String?,
    color: HabitColor?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileColor = color?.current ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val glyphColor = if (color != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val tileShape = RoundedCornerShape(8.dp)
    ChoiceGrid(items = HabitIcons.all, modifier = modifier) { icon ->
        val selected = icon.key == selectedKey
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CellSize)
                .clip(tileShape)
                .selectable(selected = selected, role = Role.RadioButton) { onSelect(icon.key) }
                .semantics { contentDescription = icon.label },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .then(
                        if (selected) {
                            Modifier.border(
                                BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface),
                                tileShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(if (selected) 4.dp else 2.dp)
                    .background(tileColor, tileShape),
            ) {
                Icon(
                    painter = painterResource(icon.drawable),
                    contentDescription = null,
                    tint = glyphColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Lays [items] out in as many equal columns as fit, row by row. Not a lazy grid on purpose: the
 * pickers live inside a scrolling form, and a lazy grid can't be nested in a vertical scroll. With
 * under a hundred items, composing them all is cheap.
 */
@Composable
private fun <T> ChoiceGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    minCellWidth: Dp = 52.dp,
    cell: @Composable (T) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = (maxWidth / minCellWidth).toInt().coerceAtLeast(1)
        Column {
            items.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { item ->
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                            cell(item)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
