package com.reevan.reevzhabitz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.addhabit.AddHabitScreen
import com.reevan.reevzhabitz.ui.common.BreadcrumbHeader
import com.reevan.reevzhabitz.ui.common.DateHeader
import com.reevan.reevzhabitz.ui.edithabit.EditHabitListScreen
import com.reevan.reevzhabitz.ui.edithabit.EditHabitScreen
import com.reevan.reevzhabitz.ui.home.HomeScreen
import com.reevan.reevzhabitz.ui.home.HomeViewModel
import com.reevan.reevzhabitz.ui.home.SortButton
import com.reevan.reevzhabitz.ui.menu.MenuScreen
import com.reevan.reevzhabitz.ui.navigation.BackStackSaver
import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.navigation.InitialBackStack
import com.reevan.reevzhabitz.ui.navigation.LeaveGuard
import com.reevan.reevzhabitz.ui.navigation.breadcrumbs
import com.reevan.reevzhabitz.ui.navigation.pop
import com.reevan.reevzhabitz.ui.navigation.popIfCurrent
import com.reevan.reevzhabitz.ui.navigation.popTo
import com.reevan.reevzhabitz.ui.navigation.pushFrom
import com.reevan.reevzhabitz.ui.removehabit.RemoveHabitScreen
import com.reevan.reevzhabitz.ui.settings.SettingsScreen
import com.reevan.reevzhabitz.ui.statistics.HabitStatisticsScreen
import com.reevan.reevzhabitz.ui.statistics.StatisticsListScreen
import com.reevan.reevzhabitz.util.TodayClock
import com.reevan.reevzhabitz.util.formatHeaderDate

/**
 * App shell: owns the back stack, picks the header for the current screen, and renders that
 * screen below it.
 *
 * Home gets the date header; every other screen gets a back arrow and a breadcrumb whose earlier
 * crumbs jump straight back. System back pops one screen; from Home it leaves the app as usual.
 */
@Composable
fun ReevzHabitzApp() {
    var backStack by rememberSaveable(stateSaver = BackStackSaver) {
        mutableStateOf(InitialBackStack)
    }
    val current = backStack.last()
    val today by TodayClock.instance.today.collectAsStateWithLifecycle()
    // Held here rather than inside HomeScreen because Home's header (sort button) needs it too.
    val home: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val homeState by home.state.collectAsStateWithLifecycle()

    // Opens a screen from the one showing in this composition; ignored if that one is no longer
    // on top (a second tap in the same frame).
    val navigate: (Destination) -> Unit = { backStack = backStack.pushFrom(current, it) }

    // Leaving the screen on top for [target] — system back, the header arrow or a breadcrumb.
    // The screen may hold it up (the Edit Habit editor, to ask before discarding changes) and
    // later approve it through [commitLeave].
    val leaveGuard = remember { LeaveGuard() }
    val leaveTo: (List<Destination>) -> Unit = { target ->
        if (!leaveGuard.intercepts(target)) backStack = target
    }
    val commitLeave: (List<Destination>) -> Unit = { backStack = it }

    // A screen finishing its own job (Create, Save, Remove) goes back to where it came from —
    // but only if it is still on top, so a breadcrumb tapped while it saved is never overruled.
    val finish: (Destination) -> () -> Unit = { screen ->
        { backStack = backStack.popIfCurrent(screen) }
    }

    BackHandler(enabled = backStack.size > 1) { leaveTo(backStack.pop()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (current == Destination.Home) {
                DateHeader(date = formatHeaderDate(today)) {
                    homeState?.let { SortButton(sort = it.sort, onClick = home::cycleSort) }
                    IconButton(onClick = { navigate(Destination.Menu) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "Menu",
                        )
                    }
                }
            } else {
                BreadcrumbHeader(
                    crumbs = backStack.breadcrumbs(),
                    onBack = { leaveTo(backStack.pop()) },
                    onCrumbClick = { index -> leaveTo(backStack.popTo(index)) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Marks the header and system bar insets as handled, so a screen's own
                // imePadding() adds only the keyboard height beyond them, not the bars twice.
                .consumeWindowInsets(innerPadding),
        ) {
            when (current) {
                Destination.Home -> HomeScreen(
                    state = homeState,
                    onMarkDone = home::markDone,
                    onMarkNotDone = home::markNotDone,
                )
                Destination.Menu -> MenuScreen(onOpen = navigate)
                Destination.AddHabit -> AddHabitScreen(
                    today = today,
                    onCreated = finish(Destination.AddHabit),
                )
                Destination.RemoveHabit -> RemoveHabitScreen(
                    today = today,
                    onRemoved = finish(Destination.RemoveHabit),
                )
                Destination.EditHabitList -> EditHabitListScreen(
                    onOpen = { navigate(Destination.EditHabit(it)) },
                )
                // Keyed by habit, so one habit's unsaved edits can never carry into another's.
                is Destination.EditHabit -> key(current.habitId) {
                    EditHabitScreen(
                        habitId = current.habitId,
                        leaveGuard = leaveGuard,
                        onLeave = commitLeave,
                        onDone = finish(current),
                    )
                }
                Destination.StatisticsList -> StatisticsListScreen(
                    onOpen = { navigate(Destination.HabitStatistics(it)) },
                )
                is Destination.HabitStatistics -> key(current.habitId) {
                    HabitStatisticsScreen(habitId = current.habitId, onGone = finish(current))
                }
                Destination.Settings -> SettingsScreen()
            }
        }
    }
}
