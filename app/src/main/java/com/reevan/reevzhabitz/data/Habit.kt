package com.reevan.reevzhabitz.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A habit. Every habit is due every day from [startDate] on; there are no weekday schedules.
 *
 * [deletedOn] is the soft-delete marker: a habit removed with "keep stats" stays here, with its
 * completions, so Statistics can still show it. Removing without keeping stats deletes the row
 * instead, and its completions go with it (see [Completion]).
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    /** Key into the habit colour palette. A key, not ARGB, so each theme can pick a variant. */
    val colorKey: String,
    /** Key into the habit icon set. Never a resource id — those change between builds. */
    val iconKey: String,
    /** First day the habit shows on Home. */
    val startDate: LocalDate,
    /** Epoch millis. Orders "newest" / "oldest" and breaks ties between same-day habits. */
    val createdAt: Long,
    /** Day the habit was removed with its stats kept; null while the habit is active. */
    val deletedOn: LocalDate? = null,
)
