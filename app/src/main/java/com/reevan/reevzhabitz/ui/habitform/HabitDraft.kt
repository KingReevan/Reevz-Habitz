package com.reevan.reevzhabitz.ui.habitform

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.util.capitalizeWords
import java.time.LocalDate

/**
 * What the Add Habit form has collected so far. Nullable fields are ones the user hasn't picked.
 *
 * Every field is required and nothing is preselected — the user assigns colour and icon
 * themselves. Start date is the exception: it defaults to tomorrow, as the spec asks.
 */
data class HabitDraft(
    val name: String,
    val description: String,
    val colorKey: String?,
    val iconKey: String?,
    val startDate: LocalDate,
) {
    val isComplete: Boolean
        get() = name.isNotBlank() && description.isNotBlank() && colorKey != null && iconKey != null

    /**
     * The habit to store. The name is trimmed and gets the same word capitalisation the name field
     * displays (the field only displays it; see HabitNameField). The description is trimmed. A
     * start date that has slipped into the past — the form was left open across midnight — moves
     * up to [today], since a habit can't start before it was created.
     */
    fun toHabit(today: LocalDate, createdAt: Long): Habit {
        require(isComplete) { "Draft is incomplete" }
        return Habit(
            name = capitalizeWords(name.trim()),
            description = description.trim(),
            colorKey = colorKey!!,
            iconKey = iconKey!!,
            startDate = maxOf(startDate, today),
            createdAt = createdAt,
        )
    }
}

/** Start From's default: habits begin tomorrow unless the user picks otherwise. */
fun defaultStartDate(today: LocalDate): LocalDate = today.plusDays(1)

/**
 * The start date the form shows and saves: the user's pick ([pickedEpochDay]) if they made one,
 * otherwise the default for the current [today]. So an untouched form left open past midnight
 * still starts the habit tomorrow, as the spec asks, rather than on the new today.
 */
fun startDateFor(pickedEpochDay: Long?, today: LocalDate): LocalDate =
    pickedEpochDay?.let(LocalDate::ofEpochDay) ?: defaultStartDate(today)

/** Start From allows today or later, never a past day. */
fun isSelectableStartDate(date: LocalDate, today: LocalDate): Boolean = !date.isBefore(today)
