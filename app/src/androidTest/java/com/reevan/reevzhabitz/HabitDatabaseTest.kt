package com.reevan.reevzhabitz

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.reevzhabitz.data.Completion
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.data.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Checks the v1 schema behaves as the data model in docs/PLAN.md says: dates round-trip, a
 * duplicate tick is harmless, and hard-deleting a habit takes its completions with it.
 *
 * In-memory, so it never touches the app's real database file.
 */
@RunWith(AndroidJUnit4::class)
class HabitDatabaseTest {

    private lateinit var db: HabitDatabase

    private val day = LocalDate.of(2026, 10, 3)

    private fun habit(name: String = "Read") = Habit(
        name = name,
        description = "",
        colorKey = "teal",
        iconKey = "book",
        startDate = day,
        createdAt = 0,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HabitDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun habit_roundTripsDates() = runBlocking {
        val id = db.habitDao().insert(habit())
        val stored = db.habitDao().getById(id)!!
        assertEquals(day, stored.startDate)
        assertNull(stored.deletedOn)

        db.habitDao().update(stored.copy(deletedOn = day.plusDays(5)))
        assertEquals(day.plusDays(5), db.habitDao().getById(id)!!.deletedOn)
    }

    @Test
    fun observeActive_excludesSoftDeleted() = runBlocking {
        val kept = db.habitDao().insert(habit("Kept"))
        val gone = db.habitDao().insert(habit("Gone"))
        db.habitDao().update(db.habitDao().getById(gone)!!.copy(deletedOn = day))

        assertEquals(listOf(kept), db.habitDao().observeActive().first().map { it.id })
    }

    @Test
    fun markDone_twice_isHarmless_andMarkNotDoneUndoes() = runBlocking {
        val id = db.habitDao().insert(habit())
        db.completionDao().markDone(Completion(id, day))
        db.completionDao().markDone(Completion(id, day))
        assertEquals(1, completionCount())

        db.completionDao().markNotDone(id, day)
        assertEquals(0, completionCount())
    }

    @Test
    fun hardDeletingAHabit_cascadesToItsCompletions() = runBlocking {
        val id = db.habitDao().insert(habit())
        val other = db.habitDao().insert(habit("Other"))
        db.completionDao().markDone(Completion(id, day))
        db.completionDao().markDone(Completion(other, day))

        db.openHelper.writableDatabase.execSQL("DELETE FROM habits WHERE id = $id")

        assertEquals(1, completionCount())
    }

    @Test
    fun observeDueOn_showsStartedActiveHabitsWithTodaysTick() = runBlocking {
        val started = db.habitDao().insert(habit("Started").copy(startDate = day.minusDays(3)))
        val startsToday = db.habitDao().insert(habit("Today").copy(startDate = day))
        db.habitDao().insert(habit("Tomorrow").copy(startDate = day.plusDays(1)))
        val deleted = db.habitDao().insert(habit("Deleted").copy(startDate = day.minusDays(3)))
        db.habitDao().update(db.habitDao().getById(deleted)!!.copy(deletedOn = day))
        db.completionDao().markDone(Completion(started, day))
        // A tick on another day must not count for this one.
        db.completionDao().markDone(Completion(startsToday, day.minusDays(1)))

        val due = db.habitDao().observeDueOn(day).first().associate { it.habit.id to it.done }
        assertEquals(mapOf(started to true, startsToday to false), due)

        // The next day nothing is ticked yet: the daily "reset".
        val tomorrow = db.habitDao().observeDueOn(day.plusDays(1)).first()
        assertEquals(3, tomorrow.size)
        assertEquals(listOf(false, false, false), tomorrow.map { it.done })
    }

    @Test
    fun settings_sortCyclesAndThemeChangesDontOverwriteEachOther() = runBlocking {
        val dao = db.appSettingsDao()
        assertNull(dao.get())

        dao.cycleHomeSort()
        assertEquals(HomeSort.NEWEST_FIRST, dao.get()!!.homeSort)
        dao.setThemeMode(ThemeMode.TOKYO_NIGHT)
        dao.cycleHomeSort()
        dao.cycleHomeSort()

        val settings = dao.get()!!
        assertEquals(HomeSort.ALPHABETICAL, settings.homeSort)
        assertEquals(ThemeMode.TOKYO_NIGHT, settings.themeMode)
    }

    private fun completionCount(): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM completions").use {
            it.moveToFirst()
            it.getInt(0)
        }
}
