package com.reevan.reevzhabitz.ui.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/*
 * The back stack is an immutable list of destinations, bottom first, held as state in
 * ReevzHabitzApp. Home is always at the bottom and is never popped — back from Home leaves the
 * app, which the system does on its own.
 *
 * Hand-rolled instead of a navigation library: the app is at most four screens deep with one
 * argument (a habit id), so push and pop are all it needs. See docs/PLAN.md, decision T2.
 */

val InitialBackStack: List<Destination> = listOf(Destination.Home)

fun List<Destination>.push(destination: Destination): List<Destination> = this + destination

/** Drops the top screen. Never empties the stack: Home stays. */
fun List<Destination>.pop(): List<Destination> = if (size > 1) dropLast(1) else this

/**
 * Jumps back to the screen at [index] — a tapped breadcrumb — dropping everything above it. An
 * index past the top changes nothing; Home (index 0) always stays.
 */
fun List<Destination>.popTo(index: Int): List<Destination> =
    take(index.coerceIn(0, lastIndex) + 1)

/**
 * Pops [screen] only if it is still on top. For a screen finishing its own job (Create, Save,
 * Remove) after an async write: if the user has already left it — say, by tapping a breadcrumb
 * while the save ran — a blind pop would take them one screen further than they chose.
 */
fun List<Destination>.popIfCurrent(screen: Destination): List<Destination> =
    if (lastOrNull() == screen) pop() else this

/** The breadcrumb trail for the current screen, e.g. ["Home", "Menu", "Add Habit"]. */
fun List<Destination>.breadcrumbs(): List<String> = map { it.label }

val BackStackSaver: Saver<List<Destination>, Any> = listSaver(
    save = { stack -> stack.map { it.route } },
    restore = { routes -> routes.map(Destination::fromRoute) },
)
