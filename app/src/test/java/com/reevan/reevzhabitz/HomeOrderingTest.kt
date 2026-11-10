package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.common.sortedFor
import com.reevan.reevzhabitz.ui.home.orderForHome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeOrderingTest {

    private fun item(
        id: Long,
        name: String,
        createdAt: Long,
        position: Int = 0,
        done: Boolean = false,
    ) = HabitOnDay(
        habit = Habit(
            id = id,
            name = name,
            description = "d",
            colorKey = "red",
            iconKey = "run",
            startDate = LocalDate.of(2026, 10, 1),
            createdAt = createdAt,
            homePosition = position,
        ),
        done = done,
    )

    private val read = item(1, "Read", createdAt = 100, position = 2)
    private val gym = item(2, "Gym", createdAt = 300, position = 0)
    private val water = item(3, "drink water", createdAt = 200, position = 1)
    private val all = listOf(read, gym, water)

    private fun names(list: List<HabitOnDay>) = list.map { it.habit.name }

    @Test
    fun home_followsTheHabitOrder_notNamesOrDates() {
        assertEquals(listOf("Gym", "drink water", "Read"), names(orderForHome(all)))
        val moved = listOf(read.copy(habit = read.habit.copy(homePosition = -1)), gym, water)
        assertEquals(listOf("Read", "Gym", "drink water"), names(orderForHome(moved)))
    }

    @Test
    fun doneHabitsSinkToTheBottom_andKeepTheOrderAmongThemselves() {
        val mixed = listOf(
            read.copy(done = true),
            gym,
            water.copy(done = true),
            item(4, "Walk", createdAt = 50, position = 3),
        )
        assertEquals(listOf("Gym", "Walk", "drink water", "Read"), names(orderForHome(mixed)))
    }

    @Test
    fun samePlace_newestFirst_thenHigherId_soTheOrderIsStable() {
        val older = item(7, "Older", createdAt = 1)
        val newer = item(5, "Newer", createdAt = 2)
        assertEquals(listOf("Newer", "Older"), names(orderForHome(listOf(older, newer))))
        val a = item(5, "Same", createdAt = 1)
        val b = item(7, "Same", createdAt = 1)
        assertEquals(listOf(7L, 5L), orderForHome(listOf(a, b)).map { it.habit.id })
        assertEquals(listOf(7L, 5L), orderForHome(listOf(b, a)).map { it.habit.id })
    }

    @Test
    fun otherLists_keepTheirSort_whateverTheHabitOrder() {
        // Edit, Remove and Statistics still go by the sort last picked, never by Habit Order.
        val habits = all.map { it.habit }
        assertEquals(
            listOf("drink water", "Gym", "Read"),
            habits.sortedFor(HomeSort.ALPHABETICAL).map { it.name },
        )
        assertEquals(listOf("Gym", "drink water", "Read"), habits.sortedFor(HomeSort.NEWEST_FIRST).map { it.name })
        assertEquals(listOf("Read", "drink water", "Gym"), habits.sortedFor(HomeSort.OLDEST_FIRST).map { it.name })
    }

    @Test
    fun otherLists_tiesFallBackToId() {
        val a = item(7, "Same", createdAt = 1).habit
        val b = item(5, "Same", createdAt = 1).habit
        HomeSort.entries.forEach { sort ->
            assertEquals(listOf(5L, 7L), listOf(a, b).sortedFor(sort).map { it.id })
        }
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals(emptyList<HabitOnDay>(), orderForHome(emptyList()))
    }
}
