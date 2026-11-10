package com.reevan.reevzhabitz.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Schema history:
 * - v1: `habits`, `completions`, `app_settings`
 * - v2: `habits.days` (the weekdays a habit is due on, a mask; default 127 = every day, so every
 *   v1 habit stays due daily). Auto-migrated.
 * - v3: `habits.homePosition` (Habit Order). Auto-migrated; [SeedHomePositions] then numbers the
 *   habits in the order Home showed them, so nothing moves.
 *
 * v1 has been on the phone, holding real habit history, since 2026-10-03: it is frozen. Never
 * edit it in place — every schema change is a version bump plus a migration here. Adding a
 * table, or a nullable column, is purely additive, so Room generates the migration from the
 * exported schemas in `app/schemas/` via `autoMigrations = [AutoMigration(from = n, to = n + 1)]`.
 * Never use fallbackToDestructiveMigration — logged habit history is not recoverable.
 */
@Database(
    entities = [
        Habit::class,
        Completion::class,
        AppSettings::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = SeedHomePositions::class),
    ],
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao

    abstract fun completionDao(): CompletionDao

    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        private const val NAME = "reevz-habitz.db"

        @Volatile
        private var instance: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
    }
}
