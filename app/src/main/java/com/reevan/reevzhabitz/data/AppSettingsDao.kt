package com.reevan.reevzhabitz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * The single settings row. Each preference is written with its own targeted UPDATE rather than by
 * rewriting the whole row, so Settings changing the theme and Home changing the sort can never
 * overwrite each other with a stale copy.
 */
@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observe(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettings?

    /** Creates the row with defaults on first use; does nothing once it exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(settings: AppSettings)

    @Query("UPDATE app_settings SET themeMode = :mode WHERE id = 1")
    suspend fun updateThemeMode(mode: ThemeMode)

    @Query("UPDATE app_settings SET homeSort = :sort WHERE id = 1")
    suspend fun updateHomeSort(sort: HomeSort)

    @Transaction
    suspend fun setThemeMode(mode: ThemeMode) {
        insertIfMissing(AppSettings())
        updateThemeMode(mode)
    }

    /** Steps Home's sort to the next order. Read and write in one transaction, so two quick taps
     *  advance twice rather than both reading the same starting value. */
    @Transaction
    suspend fun cycleHomeSort() {
        insertIfMissing(AppSettings())
        val current = get()?.homeSort ?: HomeSort.ALPHABETICAL
        updateHomeSort(current.next())
    }
}
