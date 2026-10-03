package com.reevan.reevzhabitz.data

import androidx.room.Entity
import androidx.room.ForeignKey
import java.time.LocalDate

/**
 * "This habit was done on this day." The absence of a row means not done.
 *
 * There is no daily reset: a new day simply has no rows yet, so every habit starts it unticked.
 * Rows cascade away with their habit when it is hard-deleted.
 */
@Entity(
    tableName = "completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class Completion(
    val habitId: Long,
    val date: LocalDate,
)
