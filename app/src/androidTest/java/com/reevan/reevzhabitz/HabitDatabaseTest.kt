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

    @Test
    fun remove_keepingStats_hidesTheHabitButKeepsItsHistory() = runBlocking {
        val kept = db.habitDao().insert(habit("Kept"))
        val other = db.habitDao().insert(habit("Other"))
        db.completionDao().markDone(Completion(kept, day))

        db.habitDao().remove(listOf(kept), keepStats = true, today = day.plusDays(2))

        assertEquals(day.plusDays(2), db.habitDao().getById(kept)!!.deletedOn)
        assertEquals(listOf(other), db.habitDao().observeActive().first().map { it.id })
        assertEquals(listOf(other), db.habitDao().observeDueOn(day).first().map { it.habit.id })
        assertEquals(1, completionCount())
        assertEquals(1, db.habitDao().observeDeletedCount().first())
    }

    @Test
    fun remove_withoutStats_deletesTheHabitAndItsHistory() = runBlocking {
        val gone = db.habitDao().insert(habit("Gone"))
        val other = db.habitDao().insert(habit("Other"))
        db.completionDao().markDone(Completion(gone, day))
        db.completionDao().markDone(Completion(gone, day.plusDays(1)))
        db.completionDao().markDone(Completion(other, day))

        db.habitDao().remove(listOf(gone), keepStats = false, today = day)

        assertNull(db.habitDao().getById(gone))
        assertEquals(1, completionCount())
        assertEquals(0, db.habitDao().observeDeletedCount().first())
    }

    @Test
    fun removingSeveralAtOnce_keepsTheFirstDeletionDateIfAlreadyRemoved() = runBlocking {
        val a = db.habitDao().insert(habit("A"))
        val b = db.habitDao().insert(habit("B"))
        db.habitDao().remove(listOf(a), keepStats = true, today = day)
        db.habitDao().remove(listOf(a, b), keepStats = true, today = day.plusDays(1))

        assertEquals(day, db.habitDao().getById(a)!!.deletedOn)
        assertEquals(day.plusDays(1), db.habitDao().getById(b)!!.deletedOn)
    }

    @Test
    fun purgeDeleted_clearsOnlyRemovedHabitsAndTheirHistory() = runBlocking {
        val active = db.habitDao().insert(habit("Active"))
        val removed1 = db.habitDao().insert(habit("Removed 1"))
        val removed2 = db.habitDao().insert(habit("Removed 2"))
        listOf(active, removed1, removed2).forEach {
            db.completionDao().markDone(Completion(it, day))
        }
        db.habitDao().remove(listOf(removed1, removed2), keepStats = true, today = day)

        assertEquals(2, db.habitDao().purgeDeleted())

        assertEquals(listOf(active), db.habitDao().observeActive().first().map { it.id })
        assertNull(db.habitDao().getById(removed1))
        assertNull(db.habitDao().getById(removed2))
        assertEquals(1, completionCount())
        assertEquals(0, db.habitDao().observeDeletedCount().first())
        assertEquals(0, db.habitDao().purgeDeleted())
    }

    @Test
    fun statisticsQueries_historyAndDeletedList() = runBlocking {
        val kept = db.habitDao().insert(habit("Kept"))
        val active = db.habitDao().insert(habit("Active"))
        listOf(day, day.plusDays(2)).forEach { db.completionDao().markDone(Completion(kept, it)) }
        db.completionDao().markDone(Completion(active, day))
        db.habitDao().remove(listOf(kept), keepStats = true, today = day.plusDays(3))

        assertEquals(
            setOf(day, day.plusDays(2)),
            db.completionDao().observeDatesFor(kept).first().toSet(),
        )
        assertEquals(listOf(kept), db.habitDao().observeDeleted().first().map { it.id })
        assertEquals(day.plusDays(3), db.habitDao().observeById(kept).first()!!.deletedOn)
        assertNull(db.habitDao().observeById(9999).first())
    }

    private fun completionCount(): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM completions").use {
            it.moveToFirst()
            it.getInt(0)
        }
}
