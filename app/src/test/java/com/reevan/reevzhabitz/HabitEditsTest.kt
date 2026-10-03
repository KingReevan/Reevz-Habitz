package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.ui.habitform.HabitEdits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitEditsTest {

    private val original = Habit(
        id = 7,
        name = "Drink Water",
        description = "Eight glasses.",
        colorKey = "sky",
        iconKey = "water",
        startDate = LocalDate.of(2026, 10, 4),
        createdAt = 1234,
    )
    private val unchanged = HabitEdits.from(original)

    @Test
    fun startingFromTheHabit_isNotAChange() {
        assertFalse(unchanged.changes(original))
        assertEquals(original, unchanged.applyTo(original))
    }

    @Test
    fun eachEditableField_countsAsAChange() {
        assertTrue(unchanged.copy(name = "Drink More Water").changes(original))
        assertTrue(unchanged.copy(description = "Ten glasses.").changes(original))
        assertTrue(unchanged.copy(colorKey = "teal").changes(original))
        assertTrue(unchanged.copy(iconKey = "coffee").changes(original))
    }

    @Test
    fun whitespaceAndCapitalisationOnly_isNotAChange() {
        // The stored name is normalised exactly like Add Habit's, so these save to the same thing.
        assertFalse(unchanged.copy(name = "drink water ").changes(original))
        assertFalse(unchanged.copy(description = "  Eight glasses.\n").changes(original))
    }

    @Test
    fun applyTo_normalisesAndKeepsEverythingElse() {
        val edited = HabitEdits(
            name = " go for a run ",
            description = " Before work. ",
            colorKey = "coral",
            iconKey = "run",
        ).applyTo(original.copy(deletedOn = null))

        assertEquals("Go For A Run", edited.name)
        assertEquals("Before work.", edited.description)
        assertEquals("coral", edited.colorKey)
        assertEquals("run", edited.iconKey)
        // Not editable: identity, schedule and creation time carry over untouched.
        assertEquals(original.id, edited.id)
        assertEquals(original.startDate, edited.startDate)
        assertEquals(original.createdAt, edited.createdAt)
    }

    @Test
    fun complete_needsNameAndDescription() {
        assertTrue(unchanged.isComplete)
        assertFalse(unchanged.copy(name = " ").isComplete)
        assertFalse(unchanged.copy(description = "").isComplete)
    }
}
