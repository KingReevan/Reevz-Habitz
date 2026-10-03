package com.reevan.reevzhabitz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reevan.reevzhabitz.R
import com.reevan.reevzhabitz.ui.common.BreadcrumbHeader
import com.reevan.reevzhabitz.ui.common.DateHeader
import com.reevan.reevzhabitz.ui.common.SectionPlaceholder
import com.reevan.reevzhabitz.ui.menu.MenuScreen
import com.reevan.reevzhabitz.ui.navigation.BackStackSaver
import com.reevan.reevzhabitz.ui.navigation.Destination
import com.reevan.reevzhabitz.ui.navigation.InitialBackStack
import com.reevan.reevzhabitz.ui.navigation.breadcrumbs
import com.reevan.reevzhabitz.ui.navigation.pop
import com.reevan.reevzhabitz.ui.navigation.push
import com.reevan.reevzhabitz.ui.settings.SettingsScreen
import com.reevan.reevzhabitz.ui.theme.ReevzHabitzTheme
import com.reevan.reevzhabitz.util.TodayClock
import com.reevan.reevzhabitz.util.formatHeaderDate

/**
 * App shell: owns the back stack, picks the header for the current screen, and renders that
 * screen below it.
 *
 * Home gets the date header; every other screen gets a back arrow and breadcrumb. System back
 * pops one screen; from Home it leaves the app as usual.
 */
@Composable
fun ReevzHabitzApp() {
    var backStack by rememberSaveable(stateSaver = BackStackSaver) {
        mutableStateOf(InitialBackStack)
    }
    val current = backStack.last()
    val today by TodayClock.instance.today.collectAsStateWithLifecycle()

    val navigate: (Destination) -> Unit = { backStack = backStack.push(it) }
    val goBack: () -> Unit = { backStack = backStack.pop() }

    BackHandler(enabled = backStack.size > 1, onBack = goBack)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (current == Destination.Home) {
                DateHeader(date = formatHeaderDate(today)) {
                    IconButton(onClick = { navigate(Destination.Menu) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu),
                            contentDescription = "Menu",
                        )
                    }
                }
            } else {
                BreadcrumbHeader(crumbs = backStack.breadcrumbs(), onBack = goBack)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (current) {
                Destination.Home -> SectionPlaceholder("Home", "Today's habits arrive in Phase 4.")
                Destination.Menu -> MenuScreen(onOpen = navigate)
                Destination.AddHabit -> SectionPlaceholder("Add Habit", "Coming in Phase 3.")
                Destination.RemoveHabit -> SectionPlaceholder("Remove Habit", "Coming in Phase 5.")
                Destination.EditHabitList,
                is Destination.EditHabit,
                -> SectionPlaceholder("Edit Habit", "Coming in Phase 6.")
                Destination.StatisticsList,
                is Destination.HabitStatistics,
                -> SectionPlaceholder("Statistics", "Coming in Phase 7.")
                Destination.Settings -> SettingsScreen()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReevzHabitzAppPreview() {
    ReevzHabitzTheme {
        ReevzHabitzApp()
    }
}
