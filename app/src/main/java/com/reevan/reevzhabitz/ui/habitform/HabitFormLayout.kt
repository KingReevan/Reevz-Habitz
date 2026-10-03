package com.reevan.reevzhabitz.ui.habitform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reevan.reevzhabitz.ui.theme.HabitColors
import com.reevan.reevzhabitz.ui.theme.HabitIcons

/**
 * The layout Add Habit and the Edit Habit editor share: a scrolling form above a fixed bar with
 * the one action at the bottom right — in thumb reach, and riding up above the keyboard.
 */
@Composable
fun HabitFormScaffold(
    actionLabel: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    form: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize().imePadding()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            content = form,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Button(
                onClick = onAction,
                enabled = actionEnabled,
                modifier = Modifier.height(48.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

/** Name, description, colour and icon — the four fields Add and Edit have in common. */
@Composable
fun HabitDetailsFields(
    name: TextFieldState,
    description: String,
    onDescriptionChange: (String) -> Unit,
    colorKey: String?,
    onColorChange: (String) -> Unit,
    iconKey: String?,
    onIconChange: (String) -> Unit,
) {
    val color = colorKey?.let(HabitColors::forKey)

    HabitNameField(state = name)
    HabitDescriptionField(value = description, onValueChange = onDescriptionChange)

    Column {
        FormSectionTitle(
            title = "Colour",
            detail = color?.label ?: "Pick one",
            detailIsPrompt = color == null,
        )
        HabitColorPicker(selectedKey = colorKey, onSelect = onColorChange)
    }

    Column {
        FormSectionTitle(
            title = "Icon",
            detail = iconKey?.let { HabitIcons.forKey(it).label } ?: "Pick one",
            detailIsPrompt = iconKey == null,
        )
        HabitIconPicker(selectedKey = iconKey, color = color, onSelect = onIconChange)
    }
}
