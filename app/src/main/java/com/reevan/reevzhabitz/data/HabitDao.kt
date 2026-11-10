package com.reevan.reevzhabitz.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Queries for specific screens are added in the phase that builds that screen. */
@Dao
interface HabitDao {

    @Insert
    suspend fun insert(habit: Habit): Long

    /** Add Habit's insert: the new habit goes above every other on Home. */
    @Transaction
    suspend fun insertOnTop(habit: Habit): Long =
        insert(habit.copy(homePosition = (topHomePosition() ?: 1) - 1))

    /** The first place on Home among active habits; null with none. */
    @Query("SELECT MIN(homePosition) FROM habits WHERE deletedOn IS NULL")
    suspend fun topHomePosition(): Int?

    @Query("UPDATE habits SET homePosition = :position WHERE id = :id")
    suspend fun setHomePosition(id: Long, position: Int)

    /**
     * Habit Order's Save: [ids], top first, become places 0, 1, 2… on Home. Only the place
     * changes — never name, schedule or history.
     */
    @Transaction
    suspend fun setHomeOrder(ids: List<Long>) {
        ids.forEachIndexed { position, id -> setHomePosition(id, position) }
    }

    /**
     * Rewrites the row in place (SQL UPDATE), so the habit keeps its id and every completion keyed
     * to it — editing a habit never touches its statistics. Never change this to an insert with
     * OnConflictStrategy.REPLACE: SQLite implements REPLACE as delete-then-insert, and the delete
     * would cascade and wipe the habit's whole history.
     */
    @Update
    suspend fun update(habit: Habit)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): Habit?

    /**
     * Home's list: every active habit that has started by [date] and is due on its weekday, each
     * with whether it was done that day. Room re-emits whenever `habits` or `completions` changes,
     * so ticking updates Home at once.
     */
    fun observeDueOn(date: LocalDate): Flow<List<HabitOnDay>> =
        observeDueOn(date, weekdayBit = Weekdays.bit(date.dayOfWeek))

    /** [observeDueOn] with [date]'s weekday as its bit in `habits.days` (see [Weekdays]). */
    @Query(
        """
        SELECT habits.*,
            EXISTS (
                SELECT 1 FROM completions
                WHERE completions.habitId = habits.id AND completions.date = :date
            ) AS done
        FROM habits
        WHERE deletedOn IS NULL AND startDate <= :date AND (days & :weekdayBit) != 0
        """,
    )
    fun observeDueOn(date: LocalDate, weekdayBit: Int): Flow<List<HabitOnDay>>

    /** Habits not removed, including ones whose start date is still in the future. */
    @Query("SELECT * FROM habits WHERE deletedOn IS NULL")
    fun observeActive(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: Long): Flow<Habit?>

    /** Removed habits whose stats were kept — Statistics' "Deleted" section. */
    @Query("SELECT * FROM habits WHERE deletedOn IS NOT NULL")
    fun observeDeleted(): Flow<List<Habit>>

    /** How many removed habits still have their stats kept. */
    @Query("SELECT COUNT(*) FROM habits WHERE deletedOn IS NOT NULL")
    fun observeDeletedCount(): Flow<Int>

    /**
     * Removes habits that have started by [on] but keeps them, and their completions, for
     * Statistics.
     */
    @Query(
        "UPDATE habits SET deletedOn = :on " +
            "WHERE id IN (:ids) AND deletedOn IS NULL AND startDate <= :on",
    )
    suspend fun softDeleteStarted(ids: List<Long>, on: LocalDate)

    /** Deletes those of [ids] that haven't started by [on] — they have no history to keep. */
    @Query("DELETE FROM habits WHERE id IN (:ids) AND startDate > :on")
    suspend fun hardDeleteNotStarted(ids: List<Long>, on: LocalDate)

    /** Deletes habits for good. Their completions go with them (ON DELETE CASCADE). */
    @Query("DELETE FROM habits WHERE id IN (:ids)")
    suspend fun hardDelete(ids: List<Long>)

    /**
     * Remove Habit's single entry point. With [keepStats] the habits are only marked removed as of
     * [today], so Statistics still shows them; without it they and their whole history are
     * deleted.
     *
     * A habit that hasn't started yet is deleted either way: it can't have been ticked, so there
     * is no history to keep, and Statistics never lists a habit that didn't start. Keeping it
     * would only leave a phantom "deleted habit with stats" for Settings to count.
     */
    @Transaction
    suspend fun remove(ids: List<Long>, keepStats: Boolean, today: LocalDate) {
        if (keepStats) {
            softDeleteStarted(ids, today)
            hardDeleteNotStarted(ids, today)
        } else {
            hardDelete(ids)
        }
    }

    /**
     * Settings' "Clear deleted stats": permanently deletes every removed habit and, by cascade,
     * all of its completions. Active habits are untouched. Returns how many habits were deleted.
     */
    @Query("DELETE FROM habits WHERE deletedOn IS NOT NULL")
    suspend fun purgeDeleted(): Int
}
