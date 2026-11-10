package com.reevan.reevzhabitz.data

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection

/**
 * v2 → v3 adds `habits.homePosition` (Habit Order). Room adds the column; this then numbers every
 * habit in the order Home showed it just before — the sort the user last picked with Home's old
 * sort button, A to Z if they never touched it — so Home looks exactly the same after the update
 * until a new order is saved.
 *
 * The orders are spelled out here rather than borrowed from the app, because they must stay what
 * the sort button did then, whatever the app's own ordering becomes later.
 */
class SeedHomePositions : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        val sort = connection.prepare("SELECT homeSort FROM app_settings WHERE id = 1").use {
            if (it.step()) it.getText(0) else null
        }
        val habits = connection.prepare("SELECT id, name, createdAt FROM habits").use {
            buildList { while (it.step()) add(Row(it.getLong(0), it.getText(1), it.getLong(2))) }
        }
        val order = when (sort) {
            "NEWEST_FIRST" -> compareByDescending<Row> { it.createdAt }
            "OLDEST_FIRST" -> compareBy { it.createdAt }
            else -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        }.thenBy { it.id }
        connection.prepare("UPDATE habits SET homePosition = ? WHERE id = ?").use { update ->
            habits.sortedWith(order).forEachIndexed { position, habit ->
                update.bindLong(1, position.toLong())
                update.bindLong(2, habit.id)
                update.step()
                update.reset()
            }
        }
    }

    private class Row(val id: Long, val name: String, val createdAt: Long)
}
