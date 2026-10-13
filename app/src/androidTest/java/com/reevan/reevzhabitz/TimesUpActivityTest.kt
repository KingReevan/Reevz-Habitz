package com.reevan.reevzhabitz

import android.content.Context
import android.os.SystemClock
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.reevzhabitz.timer.TimerController
import com.reevan.reevzhabitz.timer.TimerState
import com.reevan.reevzhabitz.ui.timer.TimesUpActivity
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "Time's up" page that wakes the screen when a timer rings. (Waking the screen itself is
 * Android's job, via the notification's full-screen intent; this checks the page.) Emulator only,
 * like the other device tests.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class TimesUpActivityTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val timer = TimerController.get(context)
    private val blip = 200L

    @Before
    fun nothingGoing() = timer.reset()

    @After
    fun cleanUp() {
        timer.reset()
        timer.delete(blip)
    }

    @Test
    fun whileRinging_showsTimesUp_andStopSilencesItAndCloses() {
        timer.start(blip)
        SystemClock.sleep(2 * blip)
        timer.settle()
        assertTrue(timer.state.value is TimerState.Ringing)

        ActivityScenario.launch(TimesUpActivity::class.java).use { scenario ->
            compose.waitUntilAtLeastOneExists(hasText("Time's up"), 10_000)
            compose.onNodeWithContentDescription("Stop").performClick()
            compose.waitUntil(10_000) { timer.state.value == TimerState.Idle }
            compose.waitUntil(10_000) { scenario.state == Lifecycle.State.DESTROYED }
        }
    }

    @Test
    fun withNothingRinging_closesAtOnce() {
        ActivityScenario.launch(TimesUpActivity::class.java).use { scenario ->
            compose.waitUntil(10_000) { scenario.state == Lifecycle.State.DESTROYED }
        }
    }
}
