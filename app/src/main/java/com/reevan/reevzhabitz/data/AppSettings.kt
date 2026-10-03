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
 * Home's habit order. The sort button steps through these in declaration order and wraps:
 * A–Z → newest first → oldest first → A–Z. "Newest" means most recently created.
 */
enum class HomeSort(val label: String) {
    ALPHABETICAL("A to Z"),
    NEWEST_FIRST("Newest first"),
    OLDEST_FIRST("Oldest first"),
    ;

    fun next(): HomeSort = entries[(ordinal + 1) % entries.size]
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
