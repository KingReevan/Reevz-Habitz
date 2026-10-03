package com.reevan.reevzhabitz.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** App-wide preferences. Single row, id 1. New preferences belong here. */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
