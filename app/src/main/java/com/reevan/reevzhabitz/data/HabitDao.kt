package com.reevan.reevzhabitz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Queries for specific screens are added in the phase that builds that screen. */
@Dao
interface HabitDao {

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): Habit?

    /** Habits not removed, including ones whose start date is still in the future. */
    @Query("SELECT * FROM habits WHERE deletedOn IS NULL")
    fun observeActive(): Flow<List<Habit>>
}
