package com.reevan.reevzhabitz.ui.addhabit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.data.Weekdays
import com.reevan.reevzhabitz.ui.common.rememberSubmission
import com.reevan.reevzhabitz.ui.habitform.FormSectionTitle
import com.reevan.reevzhabitz.ui.habitform.HabitDetailsFields
import com.reevan.reevzhabitz.ui.habitform.HabitDraft
import com.reevan.reevzhabitz.ui.habitform.HabitFormScaffold
import com.reevan.reevzhabitz.ui.habitform.isSelectableStartDate
import com.reevan.reevzhabitz.ui.habitform.startDateFor
import com.reevan.reevzhabitz.util.datePickerMillisToLocalDate
import com.reevan.reevzhabitz.util.formatLongDate
import com.reevan.reevzhabitz.util.toDatePickerMillis
import java.time.LocalDate

/**
 * Add Habit: name, description, start date, colour and icon, then Create — which saves the habit
 * and returns to the Menu via [onCreated].
 *
 * The icon at the bottom left opens its Advanced options page ([onOpenOptions]; the weekdays the
 * habit is due on). That page is this same screen with [showingOptions] set — the shell shows
 * both destinations from one call — so the form's state lives on while it is open, and Back
 * returns to the form exactly as it was.
 */
@Composable
fun AddHabitScreen(
    today: LocalDate,
    showingOptions: Boolean,
    onOpenOptions: () -> Unit,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHabitViewModel = viewModel(factory = AddHabitViewModel.Factory),
) {
    val name = rememberTextFieldState()
    var description by rememberSaveable { mutableStateOf("") }
    var colorKey by rememberSaveable { mutableStateOf<String?>(null) }
    var iconKey by rememberSaveable { mutableStateOf<String?>(null) }
    // The date the user picked, as an epoch day (Bundle-friendly), or null while they have not
    // picked one, in which case the start follows "tomorrow" even if the form stays open past
    // midnight.
    var pickedStartEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    // The weekdays it is due on, as a mask (see Weekdays): every day unless changed.
    var dayMask by rememberSaveable { mutableIntStateOf(Weekdays.EVERY_DAY_MASK) }
    // Held here, not in the form, so coming back from Advanced options keeps the scroll position.
    val formScroll = rememberScrollState()
    val submission = rememberSubmission(viewModel.submissions, onFinished = onCreated)

    val draft = HabitDraft(
        name = name.text.toString(),
        description = description,
        colorKey = colorKey,
        iconKey = iconKey,
        startDate = startDateFor(pickedStartEpochDay, today),
        days = Weekdays.fromMask(dayMask),
    )
    if (showingOptions) {
        AdvancedOptionsPage(
            days = draft.days,
            onDaysChange = { dayMask = Weekdays.toMask(it) },
            modifier = modifier,
        )
        return
    }
    HabitFormScaffold(
        actionLabel = "Create",
        actionEnabled = draft.isComplete && !submission.inProgress,
        onAction = { submission.start()?.let { viewModel.create(it, draft) } },
        modifier = modifier,
        scrollState = formScroll,
        barStart = { AdvancedOptionsButton(days = draft.days, onClick = onOpenOptions) },
    ) {
        HabitDetailsFields(
            name = name,
            description = description,
            onDescriptionChange = { description = it },
            colorKey = colorKey,
            onColorChange = { colorKey = it },
            iconKey = iconKey,
            onIconChange = { iconKey = it },
            afterDescription = {
                Column {
                    FormSectionTitle(title = "Start from")
                    StartDateField(
                        date = draft.startDate,
                        today = today,
                        onDateChange = { pickedStartEpochDay = it.toEpochDay() },
                    )
                }
            },
        )
    }
}

/** Shows the chosen start date; tapping it opens a date picker that refuses past days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDateField(
    date: LocalDate,
    today: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = formatLongDate(date),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = {
                Icon(painterResource(R.drawable.ic_calendar), contentDescription = null)
            },
            // Hidden from TalkBack: the layer on top speaks for the whole field, as one button.
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {},
        )
        // A read-only text field still takes focus and swallows taps, so a transparent layer on
        // top catches the tap and opens the picker instead.
        val label = "Start from, ${formatLongDate(date)}"
        Box(
            Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClickLabel = "Change start date") {
                    picking = true
                }
                .semantics { contentDescription = label },
        )
    }

    if (picking) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toDatePickerMillis(),
            yearRange = today.year..(today.year + 10),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    isSelectableStartDate(datePickerMillisToLocalDate(utcTimeMillis), today)

                override fun isSelectableYear(year: Int): Boolean = year >= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateChange(datePickerMillisToLocalDate(it)) }
                        picking = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}
