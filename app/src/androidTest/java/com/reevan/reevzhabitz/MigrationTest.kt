package com.reevan.reevzhabitz

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.data.Weekdays
import com.reevan.reevzhabitz.ui.common.HomePositionOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Upgrades old databases — v1, on the phone from 2026-10-03, and v2, from 2026-11-10 — to the
 * current schema through the app's real migrations, and checks no habit, tick or setting is lost
 * on the way, and that Home keeps showing habits in the same order.
 *
 * The old tables are built from the SQL in `app/schemas/.../<n>.json`, copied here: old versions
 * are frozen, so it never changes. Room checks the upgraded schema against the current one when it
 * opens the file, so a migration that left something out fails here too.
 *
 * Uses its own file name, never the app's real database.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 10, 3)
    private val start = day.toEpochDay()

    @Before
    @After
    fun deleteFile() {
        context.deleteDatabase(NAME)
    }

    private fun create(version: Int, schema: List<String>, fill: SQLiteDatabase.() -> Unit) {
        context.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null).use { old ->
            schema.forEach(old::execSQL)
            old.fill()
            old.version = version
        }
    }

    private fun <T> open(check: suspend (HabitDatabase) -> T): T {
        val db = Room.databaseBuilder(context, HabitDatabase::class.java, NAME).build()
        try {
            return runBlocking { check(db) }
        } finally {
            db.close()
        }
    }

    /** The names on Home's order, from the migrated habits' places. */
    private suspend fun homeOrder(db: HabitDatabase) =
        db.habitDao().observeActive().first().sortedWith(HomePositionOrder).map { it.name }

    @Test
    fun v1ToCurrent_keepsEveryHabitTickAndSetting_dueDaily_inTheSameOrder() {
        create(1, V1_SCHEMA) {
            execSQL(
                "INSERT INTO habits (id, name, description, colorKey, iconKey, startDate, " +
                    "createdAt, deletedOn) VALUES " +
                    "(1, 'Read', 'Ten pages.', 'teal', 'book', $start, 1000, NULL), " +
                    "(2, 'Old', 'Gone.', 'coral', 'run', $start, 2000, ${start + 5}), " +
                    "(3, 'Walk', 'Outside.', 'sky', 'walk', $start, 3000, NULL)",
            )
            execSQL(
                "INSERT INTO completions (habitId, date) VALUES " +
                    "(1, $start), (1, ${start + 1}), (2, $start)",
            )
            execSQL(
                "INSERT INTO app_settings (id, themeMode, homeSort) " +
                    "VALUES (1, 'TOKYO_NIGHT', 'NEWEST_FIRST')",
            )
        }

        open { db ->
            val read = db.habitDao().getById(1)!!
            assertEquals("Read", read.name)
            assertEquals("Ten pages.", read.description)
            assertEquals("teal", read.colorKey)
            assertEquals("book", read.iconKey)
            assertEquals(day, read.startDate)
            assertEquals(1000L, read.createdAt)
            assertEquals(Weekdays.EveryDay, read.days)
            val old = db.habitDao().getById(2)!!
            assertEquals(day.plusDays(5), old.deletedOn)
            assertEquals(Weekdays.EveryDay, old.days)

            assertEquals(
                listOf(day, day.plusDays(1)),
                db.completionDao().observeDatesFor(1).first().sorted(),
            )
            assertEquals(listOf(day), db.completionDao().observeDatesFor(2).first())

            // Still on Home every day of the week, with its ticks.
            (0L..6L).forEach { offset ->
                val due = db.habitDao().observeDueOn(day.plusDays(offset)).first()
                assertEquals(setOf(1L, 3L), due.map { it.habit.id }.toSet())
            }
            assertTrue(db.habitDao().observeDueOn(day).first().first { it.habit.id == 1L }.done)
            // Newest first, as the old sort button was set.
            assertEquals(listOf("Walk", "Read"), homeOrder(db))

            val settings = db.appSettingsDao().observe().first()!!
            assertEquals(ThemeMode.TOKYO_NIGHT, settings.themeMode)
            assertEquals(HomeSort.NEWEST_FIRST, settings.homeSort)
            assertEquals(CURRENT_VERSION, db.openHelper.readableDatabase.version)
        }
    }

    @Test
    fun v2ToCurrent_keepsDaysAndTicks_andNumbersHabitsInTheOrderHomeShowed() {
        val mondays = Weekdays.toMask(setOf(DayOfWeek.MONDAY))
        create(2, V2_SCHEMA) {
            execSQL(
                "INSERT INTO habits (id, name, description, colorKey, iconKey, startDate, " +
                    "createdAt, deletedOn, days) VALUES " +
                    "(1, 'Chess', 'Two games.', 'teal', 'chess', $start, 3000, NULL, 127), " +
                    "(2, 'Walk', 'Outside.', 'sky', 'walk', $start, 1000, NULL, $mondays), " +
                    "(3, 'Read', 'Ten pages.', 'coral', 'book', $start, 2000, NULL, 127)",
            )
            execSQL("INSERT INTO completions (habitId, date) VALUES (1, $start), (3, $start)")
            // The phone's setting: oldest first.
            execSQL(
                "INSERT INTO app_settings (id, themeMode, homeSort) " +
                    "VALUES (1, 'TOKYO_NIGHT', 'OLDEST_FIRST')",
            )
        }

        open { db ->
            assertEquals(listOf("Walk", "Read", "Chess"), homeOrder(db))
            // Walk (oldest) 0, Read 1, Chess (newest) 2 — listed here by id.
            assertEquals(listOf(2, 0, 1), listOf(1L, 2L, 3L).map { db.habitDao().getById(it)!!.homePosition })
            assertEquals(setOf(DayOfWeek.MONDAY), db.habitDao().getById(2)!!.days)
            assertEquals(listOf(day), db.completionDao().observeDatesFor(1).first())
            assertEquals(listOf(day), db.completionDao().observeDatesFor(3).first())
            assertEquals(HomeSort.OLDEST_FIRST, db.appSettingsDao().get()!!.homeSort)
            assertEquals(CURRENT_VERSION, db.openHelper.readableDatabase.version)
        }
    }

    @Test
    fun v2ToCurrent_sortNeverPicked_numbersHabitsAToZ_ignoringCase() {
        create(2, V2_SCHEMA) {
            execSQL(
                "INSERT INTO habits (id, name, description, colorKey, iconKey, startDate, " +
                    "createdAt, deletedOn, days) VALUES " +
                    "(1, 'walk', 'Outside.', 'sky', 'walk', $start, 1000, NULL, 127), " +
                    "(2, 'Chess', 'Two games.', 'teal', 'chess', $start, 3000, NULL, 127), " +
                    "(3, 'Read', 'Ten pages.', 'coral', 'book', $start, 2000, NULL, 127)",
            )
            // No settings row: the sort button was never touched.
        }

        open { db -> assertEquals(listOf("Chess", "Read", "walk"), homeOrder(db)) }
    }

    private companion object {
        const val NAME = "migration-test.db"
        const val CURRENT_VERSION = 3

        val ROOM_MASTER = "CREATE TABLE IF NOT EXISTS room_master_table " +
            "(id INTEGER PRIMARY KEY,identity_hash TEXT)"
        val COMPLETIONS = "CREATE TABLE IF NOT EXISTS `completions` (`habitId` INTEGER NOT NULL, " +
            "`date` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) " +
            "REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        val APP_SETTINGS = "CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, " +
            "`themeMode` TEXT NOT NULL, `homeSort` TEXT NOT NULL, PRIMARY KEY(`id`))"

        /** v1 exactly as `app/schemas/com.reevan.reevzhabitz.data.HabitDatabase/1.json` has it. */
        val V1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `colorKey` TEXT NOT NULL, " +
                "`iconKey` TEXT NOT NULL, `startDate` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `deletedOn` INTEGER)",
            COMPLETIONS,
            APP_SETTINGS,
            ROOM_MASTER,
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                "VALUES(42, 'bba597cb4061736668f6001ddb4ab61a')",
        )

        /** v2 exactly as `app/schemas/com.reevan.reevzhabitz.data.HabitDatabase/2.json` has it. */
        val V2_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `colorKey` TEXT NOT NULL, " +
                "`iconKey` TEXT NOT NULL, `startDate` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `deletedOn` INTEGER, " +
                "`days` INTEGER NOT NULL DEFAULT 127)",
            COMPLETIONS,
            APP_SETTINGS,
            ROOM_MASTER,
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                "VALUES(42, 'cc665dc272684d1fef2bf9f17ef25822')",
        )
    }
}
