package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.ui.home.DayCompletionWatcher
import com.reevan.reevzhabitz.ui.home.isDayComplete
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayCompletionTest {

    private fun item(id: Long, done: Boolean) = HabitOnDay(
        habit = Habit(
            id = id,
            name = "Habit $id",
            description = "d",
            colorKey = "red",
            iconKey = "run",
            startDate = LocalDate.of(2026, 10, 1),
            createdAt = id,
        ),
        done = done,
    )

    @Test
    fun aDayIsCompleteOnlyWithHabitsAllDone() {
        assertFalse(isDayComplete(emptyList()))
        assertFalse(isDayComplete(listOf(item(1, true), item(2, false))))
        assertTrue(isDayComplete(listOf(item(1, true), item(2, true))))
    }

    @Test
    fun tickingTheLastHabit_celebratesOnceItLands() {
        val watcher = DayCompletionWatcher()
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, false))))

        watcher.ticked(2)
        // The tap itself changes nothing until the database answers.
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, false))))
        assertTrue(watcher.completedByTick(listOf(item(1, true), item(2, true))))
        // Only once: the same finished day again is no new celebration.
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, true))))
    }

    @Test
    fun tickingAHabitThatIsNotTheLast_doesNotCelebrate() {
        val watcher = DayCompletionWatcher()
        watcher.ticked(1)
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, false))))
    }

    @Test
    fun twoQuickTicks_celebrateWhenTheSecondLands_evenInSeparateLists() {
        val watcher = DayCompletionWatcher()
        watcher.ticked(1)
        watcher.ticked(2)
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, false))))
        assertTrue(watcher.completedByTick(listOf(item(1, true), item(2, true))))
    }

    @Test
    fun twoQuickTicks_celebrateWhenBothLandInOneList() {
        val watcher = DayCompletionWatcher()
        watcher.ticked(1)
        watcher.ticked(2)
        assertTrue(watcher.completedByTick(listOf(item(1, true), item(2, true))))
    }

    @Test
    fun aDayCompletedWithoutATick_doesNotCelebrate() {
        // Opening the app on a finished day, or removing the last unticked habit elsewhere.
        val watcher = DayCompletionWatcher()
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, true))))
        assertFalse(watcher.completedByTick(listOf(item(1, true))))
    }

    @Test
    fun untickingAndTickingTheLastAgain_celebratesAgain() {
        val watcher = DayCompletionWatcher()
        watcher.ticked(2)
        assertTrue(watcher.completedByTick(listOf(item(1, true), item(2, true))))
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(2, false))))   // unticked
        watcher.ticked(2)
        assertTrue(watcher.completedByTick(listOf(item(1, true), item(2, true))))
    }

    @Test
    fun aTickForAHabitThatVanished_isForgotten() {
        val watcher = DayCompletionWatcher()
        watcher.ticked(3)
        assertFalse(watcher.completedByTick(listOf(item(1, true))))      // 3 removed mid-tick
        assertFalse(watcher.completedByTick(listOf(item(1, true), item(3, true))))
    }
}
