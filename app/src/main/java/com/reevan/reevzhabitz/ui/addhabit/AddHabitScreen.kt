package com.reevan.reevzhabitz.ui.addhabit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.habitform.FormSectionTitle
import com.reevan.reevzhabitz.ui.habitform.HabitDetailsFields
import com.reevan.reevzhabitz.ui.habitform.HabitDraft
import com.reevan.reevzhabitz.ui.habitform.HabitFormScaffold
import com.reevan.reevzhabitz.ui.habitform.defaultStartDate
import com.reevan.reevzhabitz.ui.habitform.isSelectableStartDate
import com.reevan.reevzhabitz.util.datePickerMillisToLocalDate
import com.reevan.reevzhabitz.util.formatLongDate
import com.reevan.reevzhabitz.util.toDatePickerMillis
import java.time.LocalDate

/**
 * Add Habit: name, description, colour, icon and start date, then Create — which saves the habit
 * and returns to the Menu via [onCreated].
 */
@Composable
fun AddHabitScreen(
    today: LocalDate,
    onCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddHabitViewModel = viewModel(factory = AddHabitViewModel.Factory),
) {
    val name = rememberTextFieldState()
    var description by rememberSaveable { mutableStateOf("") }
    var colorKey by rememberSaveable { mutableStateOf<String?>(null) }
    var iconKey by rememberSaveable { mutableStateOf<String?>(null) }
    // Saved as an epoch day: LocalDate isn't Bundle-friendly, a Long is.
    var startEpochDay by rememberSaveable {
        mutableLongStateOf(defaultStartDate(today).toEpochDay())
    }
    // Guards against a double tap creating the habit twice. Not saved: after a rotation the insert
    // has long finished.
    var creating by remember { mutableStateOf(false) }

    val draft = HabitDraft(
        name = name.text.toString(),
        description = description,
        colorKey = colorKey,
        iconKey = iconKey,
        startDate = LocalDate.ofEpochDay(startEpochDay),
    )
    HabitFormScaffold(
        actionLabel = "Create",
        actionEnabled = draft.isComplete && !creating,
        onAction = {
            creating = true
            viewModel.create(draft, onCreated)
        },
        modifier = modifier,
    ) {
        HabitDetailsFields(
            name = name,
            description = description,
            onDescriptionChange = { description = it },
            colorKey = colorKey,
            onColorChange = { colorKey = it },
            iconKey = iconKey,
            onIconChange = { iconKey = it },
        )
        Column {
            FormSectionTitle(title = "Start from")
            StartDateField(
                date = draft.startDate,
                today = today,
                onDateChange = { startEpochDay = it.toEpochDay() },
            )
        }
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
            modifier = Modifier.fillMaxWidth(),
        )
        // A read-only text field still takes focus and swallows taps, so a transparent layer on
        // top catches the tap and opens the picker instead.
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClickLabel = "Change start date") { picking = true },
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
