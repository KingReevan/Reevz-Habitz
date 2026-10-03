package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.navigation.InitialBackStack
import com.reevan.reevzhabitz.ui.navigation.breadcrumbs
import com.reevan.reevzhabitz.ui.navigation.pop
import com.reevan.reevzhabitz.ui.navigation.popIfCurrent
import com.reevan.reevzhabitz.ui.navigation.popTo
import com.reevan.reevzhabitz.ui.navigation.push
import com.reevan.reevzhabitz.ui.navigation.pushFrom
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
    fun push_theScreenAlreadyOnTop_doesNotStackItTwice() {
        val adding = InitialBackStack.push(Destination.Menu).push(Destination.AddHabit)
        assertEquals(adding, adding.push(Destination.AddHabit))
        val editing = adding.push(Destination.EditHabit(1))
        assertEquals(editing, editing.push(Destination.EditHabit(1)))
        // A different habit is a different screen.
        assertEquals(editing + Destination.EditHabit(2), editing.push(Destination.EditHabit(2)))
    }

    @Test
    fun pushFrom_onlyOpensFromTheScreenOnTop() {
        val menu = InitialBackStack.push(Destination.Menu)
        val adding = menu.pushFrom(Destination.Menu, Destination.AddHabit)
        assertEquals(menu + Destination.AddHabit, adding)
        // A second tap in the same frame, from the Menu that is no longer showing: ignored.
        assertEquals(adding, adding.pushFrom(Destination.Menu, Destination.RemoveHabit))
    }

    @Test
    fun pop_neverRemovesHome() {
        assertEquals(InitialBackStack, InitialBackStack.pop())
    }

    @Test
    fun popTo_jumpsBackToTheTappedCrumb() {
        val stack = InitialBackStack
            .push(Destination.Menu)
            .push(Destination.StatisticsList)
            .push(Destination.HabitStatistics(5))
        assertEquals(InitialBackStack, stack.popTo(0))
        assertEquals(listOf(Destination.Home, Destination.Menu), stack.popTo(1))
        assertEquals(stack.dropLast(1), stack.popTo(2))
        assertEquals(stack, stack.popTo(3))          // the current crumb: nowhere to go
        assertEquals(stack, stack.popTo(99))         // out of range is clamped, never throws
        assertEquals(InitialBackStack, stack.popTo(-1))
    }

    @Test
    fun popIfCurrent_onlyPopsTheScreenStillOnTop() {
        val adding = InitialBackStack.push(Destination.Menu).push(Destination.AddHabit)
        assertEquals(adding.dropLast(1), adding.popIfCurrent(Destination.AddHabit))

        // The user tapped the "Menu" crumb while Create was saving: Create's finish must not
        // then pop the Menu too.
        val alreadyLeft = adding.popTo(1)
        assertEquals(alreadyLeft, alreadyLeft.popIfCurrent(Destination.AddHabit))

        // Per-habit screens compare by habit, not just by kind.
        val editing = InitialBackStack.push(Destination.EditHabit(1))
        assertEquals(editing, editing.popIfCurrent(Destination.EditHabit(2)))
        assertEquals(InitialBackStack, editing.popIfCurrent(Destination.EditHabit(1)))
        assertEquals(InitialBackStack, InitialBackStack.popIfCurrent(Destination.Home))
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
