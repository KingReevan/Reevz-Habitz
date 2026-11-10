package com.reevan.reevzhabitz

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.data.Weekdays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Upgrades a v1 database — the one on the phone since 2026-10-03 — to the current schema through
 * the app's real migrations, and checks no habit, tick or setting is lost on the way.
 *
 * The v1 tables are built from the SQL in `app/schemas/.../1.json`, copied here: v1 is frozen, so
 * it never changes. Room checks the upgraded schema against the current one when it opens the
 * file, so a migration that left something out fails here too.
 *
 * Uses its own file name, never the app's real database.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 10, 3)

    @Before
    @After
    fun deleteFile() {
        context.deleteDatabase(NAME)
    }

    @Test
    fun v1ToCurrent_keepsEveryHabitTickAndSetting_andEveryHabitStaysDueDaily() = runBlocking {
        val start = day.toEpochDay()
        context.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null).use { v1 ->
            V1_SCHEMA.forEach(v1::execSQL)
            v1.execSQL(
                "INSERT INTO habits (id, name, description, colorKey, iconKey, startDate, " +
                    "createdAt, deletedOn) VALUES " +
                    "(1, 'Read', 'Ten pages.', 'teal', 'book', $start, 1000, NULL), " +
                    "(2, 'Old', 'Gone.', 'coral', 'run', $start, 2000, ${start + 5})",
            )
            v1.execSQL(
                "INSERT INTO completions (habitId, date) VALUES " +
                    "(1, $start), (1, ${start + 1}), (2, $start)",
            )
            v1.execSQL(
                "INSERT INTO app_settings (id, themeMode, homeSort) " +
                    "VALUES (1, 'TOKYO_NIGHT', 'NEWEST_FIRST')",
            )
            v1.version = 1
        }

        val db = Room.databaseBuilder(context, HabitDatabase::class.java, NAME).build()
        try {
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
                assertEquals(listOf(1L), due.map { it.habit.id })
            }
            assertTrue(db.habitDao().observeDueOn(day).first().single().done)

            val settings = db.appSettingsDao().observe().first()!!
            assertEquals(ThemeMode.TOKYO_NIGHT, settings.themeMode)
            assertEquals(HomeSort.NEWEST_FIRST, settings.homeSort)
            assertEquals(2, db.openHelper.readableDatabase.version)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val NAME = "migration-test.db"

        /** v1 exactly as `app/schemas/com.reevan.reevzhabitz.data.HabitDatabase/1.json` has it. */
        val V1_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `description` TEXT NOT NULL, `colorKey` TEXT NOT NULL, " +
                "`iconKey` TEXT NOT NULL, `startDate` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `deletedOn` INTEGER)",
            "CREATE TABLE IF NOT EXISTS `completions` (`habitId` INTEGER NOT NULL, " +
                "`date` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) " +
                "REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            "CREATE TABLE IF NOT EXISTS `app_settings` (`id` INTEGER NOT NULL, " +
                "`themeMode` TEXT NOT NULL, `homeSort` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) " +
                "VALUES(42, 'bba597cb4061736668f6001ddb4ab61a')",
        )
    }
}
