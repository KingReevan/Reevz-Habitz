package com.reevan.reevzhabitz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Queries for specific screens are added in the phase that builds that screen. */
@Dao
interface HabitDao {

    @Insert
    suspend fun insert(habit: Habit): Long

    @Update
    suspend fun update(habit: Habit)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): Habit?

    /**
     * Home's list: every active habit that has started by [date], each with whether it was done
     * that day. Every habit is due every day once started, so there is no schedule to check.
     * Room re-emits whenever `habits` or `completions` changes, so ticking updates Home at once.
     */
    @Query(
        """
        SELECT habits.*,
            EXISTS (
                SELECT 1 FROM completions
                WHERE completions.habitId = habits.id AND completions.date = :date
            ) AS done
        FROM habits
        WHERE deletedOn IS NULL AND startDate <= :date
        """,
    )
    fun observeDueOn(date: LocalDate): Flow<List<HabitOnDay>>

    /** Habits not removed, including ones whose start date is still in the future. */
    @Query("SELECT * FROM habits WHERE deletedOn IS NULL")
    fun observeActive(): Flow<List<Habit>>
}
