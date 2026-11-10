package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.ui.habitorder.DragStep
import com.reevan.reevzhabitz.ui.habitorder.dragStep
import com.reevan.reevzhabitz.ui.habitorder.moved
import com.reevan.reevzhabitz.ui.habitorder.shownOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class ReorderTest {

    private val order = listOf(1L, 2L, 3L, 4L)

    @Test
    fun moved_shiftsTheOnesInBetween() {
        assertEquals(listOf(2L, 3L, 1L, 4L), order.moved(0, 2))
        assertEquals(listOf(4L, 1L, 2L, 3L), order.moved(3, 0))
        assertEquals(order, order.moved(1, 1))
        assertEquals(order, order.moved(0, 9))     // out of range: nothing moves
        assertEquals(order, order.moved(-1, 0))
    }

    @Test
    fun shown_isTheStoredOrderUntilSomethingMoves() {
        assertEquals(order, shownOrder(null, order))
        assertEquals(listOf(4L, 3L, 2L, 1L), shownOrder(listOf(4L, 3L, 2L, 1L), order))
    }

    @Test
    fun shown_dropsHabitsThatWentAndPutsNewOnesOnTop() {
        // 2 was removed elsewhere; 5 appeared.
        assertEquals(listOf(5L, 4L, 3L, 1L), shownOrder(listOf(4L, 3L, 2L, 1L), listOf(5L, 1L, 3L, 4L)))
    }

    @Test
    fun drag_staysPut_untilItCoversMoreThanHalfAWay() {
        assertEquals(DragStep(order, 49f), dragStep(order, 2L, 49f, slot = 100f))
        assertEquals(DragStep(order, -49f), dragStep(order, 2L, -49f, slot = 100f))
    }

    @Test
    fun drag_swapsPastEachNeighbourItCovers_keepingTheCardUnderTheFinger() {
        assertEquals(DragStep(listOf(1L, 3L, 2L, 4L), -40f), dragStep(order, 2L, 60f, slot = 100f))
        // A fast flick past two neighbours at once.
        assertEquals(DragStep(listOf(1L, 3L, 4L, 2L), 20f), dragStep(order, 2L, 220f, slot = 100f))
        assertEquals(DragStep(listOf(3L, 1L, 2L, 4L), 30f), dragStep(order, 3L, -170f, slot = 100f))
    }

    @Test
    fun drag_stopsAtEitherEnd() {
        assertEquals(DragStep(order, -300f), dragStep(order, 1L, -300f, slot = 100f))
        assertEquals(DragStep(listOf(2L, 3L, 4L, 1L), 200f), dragStep(order, 1L, 500f, slot = 100f))
    }

    @Test
    fun drag_unknownHabitOrNoSize_changesNothing() {
        assertEquals(DragStep(order, 80f), dragStep(order, 9L, 80f, slot = 100f))
        assertEquals(DragStep(order, 80f), dragStep(order, 2L, 80f, slot = 0f))
    }
}
