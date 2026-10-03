package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.navigation.InitialBackStack
import com.reevan.reevzhabitz.ui.navigation.breadcrumbs
import com.reevan.reevzhabitz.ui.navigation.pop
import com.reevan.reevzhabitz.ui.navigation.push
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackStackTest {

    @Test
    fun startsAtHome() {
        assertEquals(listOf(Destination.Home), InitialBackStack)
    }

    @Test
    fun pushThenPop_returnsToWhereItWas() {
        val menu = InitialBackStack.push(Destination.Menu)
        val add = menu.push(Destination.AddHabit)
        assertEquals(Destination.AddHabit, add.last())
        assertEquals(menu, add.pop())
        assertEquals(InitialBackStack, add.pop().pop())
    }

    @Test
    fun pop_neverRemovesHome() {
        assertEquals(InitialBackStack, InitialBackStack.pop())
    }

    @Test
    fun breadcrumbs_followTheStack() {
        val stack = InitialBackStack.push(Destination.Menu).push(Destination.AddHabit)
        assertEquals(listOf("Home", "Menu", "Add Habit"), stack.breadcrumbs())
    }

    @Test
    fun menuEntries_areTheFiveSectionsInSpecOrder() {
        assertEquals(
            listOf("Add Habit", "Remove Habit", "Edit Habit", "Statistics", "Settings"),
            Destination.menuEntries.map { it.label },
        )
    }

    @Test
    fun everyDestination_roundTripsThroughItsRoute() {
        val all = listOf(
            Destination.Home,
            Destination.Menu,
            Destination.AddHabit,
            Destination.RemoveHabit,
            Destination.EditHabitList,
            Destination.StatisticsList,
            Destination.Settings,
            Destination.EditHabit(42),
            Destination.HabitStatistics(7),
        )
        all.forEach { assertEquals(it, Destination.fromRoute(it.route)) }
        assertEquals(all.size, all.map { it.route }.toSet().size)
    }

    @Test
    fun fromRoute_rejectsGarbage() {
        listOf("", "nope", "edit/", "edit/abc", "stats/1/2", "home/3").forEach { route ->
            assertThrows(IllegalArgumentException::class.java) { Destination.fromRoute(route) }
        }
    }
}
