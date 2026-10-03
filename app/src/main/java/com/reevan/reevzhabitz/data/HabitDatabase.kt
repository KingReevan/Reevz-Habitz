package com.reevan.reevzhabitz.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema history:
 * - v1: `app_settings`
 *
 * Version bumps must add a migration here. Adding a table, or a nullable column, is purely
 * additive, so Room generates the migration from the exported schemas in `app/schemas/` via
 * `autoMigrations = [AutoMigration(from = n, to = n + 1)]`. Never use
 * fallbackToDestructiveMigration — logged habit history is not recoverable.
 */
@Database(
    entities = [
        AppSettings::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HabitDatabase : RoomDatabase() {

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
