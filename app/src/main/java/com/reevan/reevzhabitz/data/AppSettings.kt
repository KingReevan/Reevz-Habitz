package com.reevan.reevzhabitz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The app's four themes. There is deliberately no "follow system" option. */
enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    VSCODE_DARK("VS Code Dark"),
    TOKYO_NIGHT("Tokyo Night"),
}

/**
 * The order of the Edit Habit, Remove Habit and Statistics lists. "Newest" means most recently
 * created. It was set by Home's sort button, which Habit Order replaced on 2026-11-10; the lists
 * keep whichever order was last picked. Home itself follows [Habit.homePosition].
 */
enum class HomeSort {
    ALPHABETICAL,
    NEWEST_FIRST,
    OLDEST_FIRST,
}

/** App-wide preferences. Single row, id 1. New preferences belong here. */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Remembered across restarts. */
    val homeSort: HomeSort = HomeSort.ALPHABETICAL,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
