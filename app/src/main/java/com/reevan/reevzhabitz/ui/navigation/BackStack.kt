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

/** The breadcrumb trail for the current screen, e.g. ["Home", "Menu", "Add Habit"]. */
fun List<Destination>.breadcrumbs(): List<String> = map { it.label }

val BackStackSaver: Saver<List<Destination>, Any> = listSaver(
    save = { stack -> stack.map { it.route } },
    restore = { routes -> routes.map(Destination::fromRoute) },
)
