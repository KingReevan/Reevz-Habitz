package com.reevan.reevzhabitz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CompletionDao {

    /** Ticking an already-ticked habit is a no-op, not an error. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markDone(completion: Completion)

    @Query("DELETE FROM completions WHERE habitId = :habitId AND date = :date")
    suspend fun markNotDone(habitId: Long, date: LocalDate)

    /** Every day [habitId] was done — its whole history, for Statistics. */
    @Query("SELECT date FROM completions WHERE habitId = :habitId")
    fun observeDatesFor(habitId: Long): Flow<List<LocalDate>>
}
