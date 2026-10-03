package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitOnDay
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.ui.home.orderForHome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeOrderingTest {

    private fun item(id: Long, name: String, createdAt: Long, done: Boolean = false) = HabitOnDay(
        habit = Habit(
            id = id,
            name = name,
            description = "d",
            colorKey = "red",
            iconKey = "run",
            startDate = LocalDate.of(2026, 10, 1),
            createdAt = createdAt,
        ),
        done = done,
    )

    private val read = item(1, "Read", createdAt = 100)
    private val gym = item(2, "Gym", createdAt = 300)
    private val water = item(3, "drink water", createdAt = 200)
    private val all = listOf(read, gym, water)

    private fun names(list: List<HabitOnDay>) = list.map { it.habit.name }

    @Test
    fun alphabetical_ignoresCase() {
        assertEquals(listOf("drink water", "Gym", "Read"), names(orderForHome(all, HomeSort.ALPHABETICAL)))
    }

    @Test
    fun newestFirst_isByCreationTimeDescending() {
        assertEquals(listOf("Gym", "drink water", "Read"), names(orderForHome(all, HomeSort.NEWEST_FIRST)))
    }

    @Test
    fun oldestFirst_isByCreationTimeAscending() {
        assertEquals(listOf("Read", "drink water", "Gym"), names(orderForHome(all, HomeSort.OLDEST_FIRST)))
    }

    @Test
    fun doneHabitsSinkToTheBottom_andKeepTheSortAmongThemselves() {
        val mixed = listOf(
            read.copy(done = true),
            gym,
            water.copy(done = true),
            item(4, "Walk", createdAt = 50),
        )
        assertEquals(
            listOf("Gym", "Walk", "drink water", "Read"),
            names(orderForHome(mixed, HomeSort.ALPHABETICAL)),
        )
        assertEquals(
            listOf("Gym", "Walk", "drink water", "Read"),
            names(orderForHome(mixed, HomeSort.NEWEST_FIRST)),
        )
        assertEquals(
            listOf("Walk", "Gym", "Read", "drink water"),
            names(orderForHome(mixed, HomeSort.OLDEST_FIRST)),
        )
    }

    @Test
    fun ties_fallBackToId_soTheOrderIsStable() {
        val a = item(7, "Same", createdAt = 1)
        val b = item(5, "Same", createdAt = 1)
        HomeSort.entries.forEach { sort ->
            assertEquals(listOf(5L, 7L), orderForHome(listOf(a, b), sort).map { it.habit.id })
        }
    }

    @Test
    fun sortButton_cyclesAzThenNewestThenOldestThenBack() {
        assertEquals(HomeSort.NEWEST_FIRST, HomeSort.ALPHABETICAL.next())
        assertEquals(HomeSort.OLDEST_FIRST, HomeSort.NEWEST_FIRST.next())
        assertEquals(HomeSort.ALPHABETICAL, HomeSort.OLDEST_FIRST.next())
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals(emptyList<HabitOnDay>(), orderForHome(emptyList(), HomeSort.ALPHABETICAL))
    }
}
