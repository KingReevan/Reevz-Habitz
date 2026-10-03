package com.reevan.reevzhabitz.ui.navigation

/**
 * Every screen in the app. [label] is the screen's breadcrumb, and for the five Menu screens also
 * its Menu button text.
 *
 * [route] is a string form used only to save the back stack across process death; [fromRoute]
 * reverses it.
 */
sealed class Destination(val label: String, val route: String) {
    data object Home : Destination("Home", "home")
    data object Menu : Destination("Menu", "menu")
    data object AddHabit : Destination("Add Habit", "add")
    data object RemoveHabit : Destination("Remove Habit", "remove")
    data object EditHabitList : Destination("Edit Habit", "edit")
    data object StatisticsList : Destination("Statistics", "stats")
    data object Settings : Destination("Settings", "settings")

    // The per-habit screens have short generic crumbs, by the owner's choice; the screens
    // themselves show which habit they are about.
    data class EditHabit(val habitId: Long) : Destination("Edit", "edit/$habitId")
    data class HabitStatistics(val habitId: Long) : Destination("Stats", "stats/$habitId")

    // Both lists are lazy on purpose. The companion is initialised as part of Destination itself,
    // before its nested objects exist, so an eager listOf(Home, ...) here captures nulls.
    companion object {
        /** The Menu screen's buttons, top to bottom. */
        val menuEntries: List<Destination> by lazy {
            listOf(AddHabit, RemoveHabit, EditHabitList, StatisticsList, Settings)
        }

        private val fixed: List<Destination> by lazy {
            listOf(Home, Menu, AddHabit, RemoveHabit, EditHabitList, StatisticsList, Settings)
        }

        fun fromRoute(route: String): Destination {
            fixed.firstOrNull { it.route == route }?.let { return it }
            val (prefix, id) = route.split('/', limit = 2).let { it[0] to it.getOrNull(1) }
            val habitId = requireNotNull(id?.toLongOrNull()) { "Unknown route: $route" }
            return when (prefix) {
                "edit" -> EditHabit(habitId)
                "stats" -> HabitStatistics(habitId)
                else -> throw IllegalArgumentException("Unknown route: $route")
            }
        }
    }
}
