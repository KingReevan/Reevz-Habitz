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

/** App-wide preferences. Single row, id 1. New preferences belong here. */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: ThemeMode = ThemeMode.DARK,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
