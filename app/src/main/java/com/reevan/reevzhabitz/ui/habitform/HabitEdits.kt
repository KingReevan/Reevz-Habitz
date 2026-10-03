package com.reevan.reevzhabitz.ui.habitform

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.util.capitalizeWords

/**
 * What the Edit Habit editor holds. Only these four fields are editable; start date, creation time
 * and deletion state always carry over from the original untouched.
 */
data class HabitEdits(
    val name: String,
    val description: String,
    val colorKey: String,
    val iconKey: String,
) {
    /** Same rule as Add Habit: every field filled in. */
    val isComplete: Boolean
        get() = name.isNotBlank() && description.isNotBlank()

    /**
     * [original] with these edits applied, normalised exactly as Add Habit normalises a new habit
     * (trimmed, name capitalised), so an edit that only adds a trailing space isn't a change.
     */
    fun applyTo(original: Habit): Habit = original.copy(
        name = capitalizeWords(name.trim()),
        description = description.trim(),
        colorKey = colorKey,
        iconKey = iconKey,
    )

    /** Whether saving would change anything. */
    fun changes(original: Habit): Boolean = applyTo(original) != original

    companion object {
        fun from(habit: Habit) = HabitEdits(
            name = habit.name,
            description = habit.description,
            colorKey = habit.colorKey,
            iconKey = habit.iconKey,
        )
    }
}
