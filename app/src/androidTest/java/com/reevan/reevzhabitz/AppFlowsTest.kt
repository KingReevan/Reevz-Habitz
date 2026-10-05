package com.reevan.reevzhabitz

import android.content.Context
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reevan.reevzhabitz.data.Completion
import com.reevan.reevzhabitz.data.Habit
import com.reevan.reevzhabitz.data.HabitDatabase
import com.reevan.reevzhabitz.data.HomeSort
import com.reevan.reevzhabitz.data.ThemeMode
import com.reevan.reevzhabitz.ui.common.BREADCRUMBS_TAG
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * End-to-end flows through the real app: real Activity, real database, real "today".
 *
 * **Wipes the app's database before every test. Emulator only** — run with
 * `ANDROID_SERIAL=emulator-5554` whenever the phone is plugged in (see CLAUDE.md).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppFlowsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = HabitDatabase.getInstance(context)
    private val today: LocalDate = LocalDate.now()

    /** Runs before the Activity starts, so every test opens on an empty app. */
    @get:Rule(order = 0)
    val emptyDatabase = object : ExternalResource() {
        override fun before() = db.clearAllTables()
    }

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    // ---- helpers ---------------------------------------------------------------------------

    private fun habit(
        name: String,
        start: LocalDate = today,
        createdAt: Long = System.currentTimeMillis(),
        color: String = "teal",
        icon: String = "run",
    ) = Habit(
        name = name,
        description = "About $name.",
        colorKey = color,
        iconKey = icon,
        startDate = start,
        createdAt = createdAt,
    )

    private fun insert(vararg habits: Habit): List<Long> = runBlocking {
        habits.map { db.habitDao().insert(it) }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 10_000) =
        rule.waitUntilAtLeastOneExists(hasText(text), timeoutMillis)

    private fun waitFor(timeoutMillis: Long = 10_000, condition: () -> Boolean) =
        rule.waitUntil(timeoutMillis) { condition() }

    private fun top(text: String): Float =
        rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    /** The breadcrumb trail as shown, e.g. ["Home", "Menu", "Settings"]. */
    private fun crumbs(): List<String> {
        val trail = rule.onNodeWithTag(BREADCRUMBS_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val texts = mutableListOf<String>()
        fun walk(node: SemanticsNode) {
            node.config.getOrNull(SemanticsProperties.Text)?.let { parts ->
                texts += parts.joinToString("") { it.text }
            }
            node.children.forEach(::walk)
        }
        walk(trail)
        return texts
    }

    private fun waitForCrumbs(vararg expected: String) =
        rule.waitUntil(10_000) { runCatching { crumbs() == expected.toList() }.getOrDefault(false) }

    private fun crumb(name: String) =
        rule.onNode(hasText(name) and hasAnyAncestor(hasTestTag(BREADCRUMBS_TAG)))

    private fun openMenu() {
        rule.waitUntilAtLeastOneExists(hasContentDescription("Menu"), 10_000)
        rule.onNodeWithContentDescription("Menu").performClick()
        waitForText("ADD HABIT")
    }

    private fun openFromMenu(button: String) {
        openMenu()
        rule.onNodeWithText(button).performClick()
    }

    /** The stored habit with this exact name, deleted or not. */
    private fun stored(name: String): Habit? {
        val id = db.openHelper.readableDatabase
            .query("SELECT id FROM habits WHERE name = ?", arrayOf(name))
            .use { if (it.moveToFirst()) it.getLong(0) else return null }
        return runBlocking { db.habitDao().getById(id) }
    }

    private fun isDone(id: Long, day: LocalDate = today): Boolean =
        db.openHelper.readableDatabase.query(
            "SELECT 1 FROM completions WHERE habitId = ? AND date = ?",
            arrayOf(id, day.toEpochDay()),
        ).use { it.moveToFirst() }

    // ---- Home --------------------------------------------------------------------------------

    @Test
    fun home_showsOnlyHabitsDueToday() {
        insert(
            habit("Started Earlier", start = today.minusDays(5)),
            habit("Starts Today"),
            habit("Starts Tomorrow", start = today.plusDays(1)),
        )
        waitForText("Started Earlier")
        rule.onNodeWithText("Starts Today").assertIsDisplayed()
        rule.onNodeWithText("Starts Tomorrow").assertDoesNotExist()
    }

    @Test
    fun home_emptyState() {
        waitForText("No habits for today.\nAdd one from the menu.")
    }

    @Test
    fun home_tickingCrossesOffAndSinksToTheBottom_untickAsksFirst() {
        val (alpha, _) = insert(habit("Alpha"), habit("Bravo"))
        waitForText("Alpha")
        assertTrue("A–Z: Alpha first", top("Alpha") < top("Bravo"))

        rule.onNodeWithContentDescription("Alpha").performClick()
        waitFor { isDone(alpha) }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Alpha").assertIsOn()
        waitFor { top("Alpha") > top("Bravo") }

        // Untick: asks first. Cancel keeps it done.
        rule.onNodeWithContentDescription("Alpha").performClick()
        waitForText("Mark as not done?")
        rule.onNodeWithText("Cancel").performClick()
        assertTrue(isDone(alpha))
        rule.onNodeWithContentDescription("Alpha").assertIsOn()

        // Confirm makes it not done, and it rises back above Bravo.
        rule.onNodeWithContentDescription("Alpha").performClick()
        waitForText("Mark as not done?")
        rule.onNodeWithText("Confirm").performClick()
        waitFor { !isDone(alpha) }
        rule.onNodeWithContentDescription("Alpha").assertIsOff()
        waitFor { top("Alpha") < top("Bravo") }
    }

    private fun exists(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun home_tickingTheLastHabit_popsAStar_thenTheCardStaysUntilAnUntick() {
        val (alpha, bravo) = insert(habit("Alpha"), habit("Bravo"))
        waitForText("Alpha")

        rule.onNodeWithContentDescription("Alpha").performClick()
        waitFor { isDone(alpha) }
        rule.waitForIdle()
        assertFalse("Not the last habit: no star", exists("celebrationStar"))
        assertFalse("Not all done: no card", exists("allDoneCard"))

        rule.onNodeWithContentDescription("Bravo").performClick()
        waitFor { exists("celebrationStar") }
        waitForText("Everything is complete. Fantastic!")
        // The star is only a moment; the card stays.
        waitFor { !exists("celebrationStar") }
        rule.onNodeWithTag("allDoneCard").assertIsDisplayed()
        assertTrue(isDone(bravo))

        rule.onNodeWithContentDescription("Alpha").performClick()
        waitForText("Mark as not done?")
        rule.onNodeWithText("Confirm").performClick()
        waitFor { !exists("allDoneCard") }
        assertFalse(exists("celebrationStar"))
    }

    @Test
    fun home_aDayAlreadyComplete_showsTheCardButNoStar() {
        val (id) = insert(habit("Finished"))
        runBlocking { db.completionDao().markDone(Completion(id, today)) }
        waitForText("Everything is complete. Fantastic!")
        rule.waitForIdle()
        assertFalse(exists("celebrationStar"))
    }

    @Test
    fun home_untickPromptCloses_ifTheHabitStopsBeingDoneWhileItIsOpen() {
        // What midnight does to an open "Mark as not done?" prompt: the new day has no tick, so
        // the habit is no longer done. Simulated by removing today's tick directly.
        val (id) = insert(habit("Stale Prompt"))
        runBlocking { db.completionDao().markDone(Completion(id, today)) }
        waitForText("Stale Prompt")
        rule.waitUntil(10_000) {
            runCatching { rule.onNodeWithContentDescription("Stale Prompt").assertIsOn() }.isSuccess
        }
        rule.onNodeWithContentDescription("Stale Prompt").performClick()
        waitForText("Mark as not done?")

        runBlocking { db.completionDao().markNotDone(id, today) }

        rule.waitUntil(10_000) {
            rule.onAllNodesWithText("Mark as not done?").fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun home_sortCyclesThroughThreeOrdersAndIsRemembered() {
        val now = System.currentTimeMillis()
        insert(
            habit("Middle", createdAt = now - 2_000),
            habit("Zed Oldest", createdAt = now - 3_000),
            habit("Apple Newest", createdAt = now - 1_000),
        )
        waitForText("Middle")
        fun order() = listOf("Apple Newest", "Middle", "Zed Oldest").sortedBy { top(it) }

        assertEquals(listOf("Apple Newest", "Middle", "Zed Oldest"), order())
        rule.onNodeWithContentDescription("Sorted A to Z. Change sort").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Sorted Newest first. Change sort"))
        waitFor { order() == listOf("Apple Newest", "Middle", "Zed Oldest") }

        rule.onNodeWithContentDescription("Sorted Newest first. Change sort").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Sorted Oldest first. Change sort"))
        waitFor { order() == listOf("Zed Oldest", "Middle", "Apple Newest") }
        assertEquals(HomeSort.OLDEST_FIRST, runBlocking { db.appSettingsDao().get()!!.homeSort })

        rule.onNodeWithContentDescription("Sorted Oldest first. Change sort").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Sorted A to Z. Change sort"))
    }

    // ---- Navigation ------------------------------------------------------------------------

    @Test
    fun navigation_breadcrumbsAndBack() {
        openMenu()
        waitForCrumbs("Home", "Menu")
        rule.onNodeWithText("SETTINGS").performClick()
        waitForCrumbs("Home", "Menu", "Settings")
        rule.onNodeWithContentDescription("Back").performClick()
        waitForText("ADD HABIT")
        Espresso.pressBack()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Menu"))
    }

    @Test
    fun breadcrumbs_jumpStraightToAnyEarlierScreen() {
        insert(habit("Crumbed", start = today.minusDays(1)))
        openFromMenu("STATISTICS")
        waitForText("Crumbed")
        rule.onNodeWithText("Crumbed").performClick()
        waitForCrumbs("Home", "Menu", "Statistics", "Stats")
        crumb("Stats").assertHasNoClickAction()                 // the current screen

        crumb("Statistics").performClick()
        waitForCrumbs("Home", "Menu", "Statistics")
        rule.onNodeWithText("Crumbed").performClick()
        waitForCrumbs("Home", "Menu", "Statistics", "Stats")

        crumb("Menu").performClick()                            // two screens in one tap
        waitForCrumbs("Home", "Menu")
        rule.onNodeWithText("SETTINGS").performClick()
        waitForCrumbs("Home", "Menu", "Settings")

        crumb("Home").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Menu"))
        rule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun breadcrumbs_fromAnEditorWithChanges_askThenGoWhereTapped() {
        val (id) = insert(habit("Guarded", color = "coral"))
        openFromMenu("EDIT HABIT")
        waitForText("Guarded")
        rule.onNodeWithText("Guarded").performClick()
        waitForText("Save")

        // Nothing changed: a crumb leaves at once.
        crumb("Menu").performClick()
        waitForCrumbs("Home", "Menu")

        rule.onNodeWithText("EDIT HABIT").performClick()
        waitForText("Guarded")
        rule.onNodeWithText("Guarded").performClick()
        waitForText("Save")
        rule.onNodeWithContentDescription("Green").performScrollTo().performClick()

        // Changed: asks first. Keep editing stays put with the change intact.
        crumb("Home").performClick()
        waitForText("Discard changes?")
        rule.onNodeWithText("Keep editing").performClick()
        waitForCrumbs("Home", "Menu", "Edit Habit", "Edit")
        rule.onNodeWithText("Save").assertIsEnabled()

        // Discard goes to the crumb that was tapped — the Menu, two screens up — not one back.
        crumb("Menu").performClick()
        waitForText("Discard changes?")
        rule.onNodeWithText("Discard").performClick()
        waitForCrumbs("Home", "Menu")
        rule.onNodeWithText("ADD HABIT").assertIsDisplayed()
        assertEquals("coral", runBlocking { db.habitDao().getById(id)!!.colorKey })
    }

    @Test
    fun breadcrumbs_discardDialogSurvivesRotationAndKeepsItsTarget() {
        insert(habit("Rotated", color = "coral"))
        openFromMenu("EDIT HABIT")
        waitForText("Rotated")
        rule.onNodeWithText("Rotated").performClick()
        waitForText("Save")
        rule.onNodeWithContentDescription("Green").performScrollTo().performClick()
        crumb("Home").performClick()
        waitForText("Discard changes?")

        rule.activityRule.scenario.recreate()
        waitForText("Discard changes?")
        rule.onNodeWithText("Discard").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Menu"))   // Home, as tapped
        rule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    // ---- Add Habit -------------------------------------------------------------------------

    @Test
    fun addHabit_everyFieldRequired_capitalises_andCreatesForTomorrow() {
        openFromMenu("ADD HABIT")
        waitForText("Create")
        val create = rule.onNodeWithText("Create")
        create.assertIsNotEnabled()

        // Start From sits right under Description, above the tall colour and icon pickers.
        assertTrue(top("Description") < top("Start from"))
        assertTrue(top("Start from") < top("Colour"))
        assertTrue(top("Colour") < top("Icon"))

        rule.onNodeWithText("Habit name").performTextInput("drink water")
        create.assertIsNotEnabled()
        rule.onNodeWithText("Description").performTextInput("Eight glasses.")
        create.assertIsNotEnabled()
        rule.onNodeWithContentDescription("Sky").performScrollTo().performClick()
        create.assertIsNotEnabled()
        rule.onNodeWithContentDescription("Water").performScrollTo().performClick()
        create.assertIsEnabled()
        // Shown capitalised as typed. Scrolled back into view: picking the icon scrolled past it.
        rule.onNodeWithText("Drink Water").performScrollTo().assertIsDisplayed()

        create.performClick()
        waitForText("ADD HABIT")                                   // back on the Menu
        val saved = stored("Drink Water")!!
        assertEquals("Eight glasses.", saved.description)
        assertEquals("sky", saved.colorKey)
        assertEquals("water", saved.iconKey)
        assertEquals(today.plusDays(1), saved.startDate)
    }

    @Test
    fun addHabit_doubleTapOnCreate_makesOneHabitAndStopsAtTheMenu() {
        openFromMenu("ADD HABIT")
        waitForText("Create")
        rule.onNodeWithText("Habit name").performTextInput("only once")
        rule.onNodeWithText("Description").performTextInput("Just one.")
        rule.onNodeWithContentDescription("Sky").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Water").performScrollTo().performClick()

        // Both taps arrive before the screen can redraw and disable the button.
        rule.onNodeWithText("Create").performTouchInput {
            click()
            click()
        }

        waitForText("ADD HABIT")
        rule.waitForIdle()
        val count = db.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM habits WHERE name = 'Only Once'")
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(1, count)
        rule.onNodeWithText("ADD HABIT").assertIsDisplayed()   // not popped past the Menu
    }

    @Test
    fun addHabit_rotatingRightAfterCreate_stillLeavesOnceWithOneHabit() {
        openFromMenu("ADD HABIT")
        waitForText("Create")
        rule.onNodeWithText("Habit name").performTextInput("rotated create")
        rule.onNodeWithText("Description").performTextInput("Mid-save rotation.")
        rule.onNodeWithContentDescription("Sky").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Water").performScrollTo().performClick()

        // Rotate straight after Create, while the insert may still be running. Whichever finishes
        // first, the new screen must still leave, and Create must not be live again meanwhile.
        rule.onNodeWithText("Create").performClick()
        rule.activityRule.scenario.recreate()

        waitForText("ADD HABIT")
        rule.waitForIdle()
        val count = db.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM habits WHERE name = 'Rotated Create'")
            .use { it.moveToFirst(); it.getInt(0) }
        assertEquals(1, count)
        waitForCrumbs("Home", "Menu")
    }

    @Test
    fun editHabit_doubleTapOnSave_returnsToTheListOnce() {
        insert(habit("Twice Saved", color = "coral"))
        openFromMenu("EDIT HABIT")
        waitForText("Twice Saved")
        rule.onNodeWithText("Twice Saved").performClick()
        waitForText("Save")
        rule.onNodeWithContentDescription("Green").performScrollTo().performClick()
        rule.onNodeWithText("Save").performTouchInput {
            click()
            click()
        }
        waitForCrumbs("Home", "Menu", "Edit Habit")
        rule.waitForIdle()
        assertEquals(listOf("Home", "Menu", "Edit Habit"), crumbs())
    }

    @Test
    fun addHabit_formSurvivesRotation_andIsFreshOnNextVisit() {
        openFromMenu("ADD HABIT")
        waitForText("Create")
        rule.onNodeWithText("Habit name").performTextInput("keep me")
        rule.onNodeWithContentDescription("Coral").performScrollTo().performClick()

        rule.activityRule.scenario.recreate()
        waitForText("Keep Me")
        rule.onNodeWithText("Coral").performScrollTo().assertIsDisplayed()  // "Colour  Coral"

        rule.onNodeWithContentDescription("Back").performClick()
        waitForText("ADD HABIT")
        rule.onNodeWithText("ADD HABIT").performClick()
        waitForText("Create")
        rule.onNodeWithText("Keep Me").assertDoesNotExist()
    }

    // ---- Remove Habit ----------------------------------------------------------------------

    @Test
    fun removeHabit_keepStatsThenDeleteForGood() {
        val (kept, gone) = insert(habit("Kept Habit"), habit("Gone Habit"))
        runBlocking { db.completionDao().markDone(Completion(gone, today)) }

        openFromMenu("REMOVE HABIT")
        waitForText("Kept Habit")
        rule.onNodeWithText("Remove Habit(s)").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Kept Habit").performClick()
        rule.onNodeWithText("1 selected").assertIsDisplayed()
        rule.onNodeWithText("Remove Habit(s)").performClick()
        waitForText("Remove this habit?")
        rule.onNodeWithText("Keep stats").assertIsOn()
        rule.onNodeWithText("Remove").performClick()
        waitForText("ADD HABIT")
        waitFor { runBlocking { db.habitDao().getById(kept)?.deletedOn } == today }

        rule.onNodeWithText("REMOVE HABIT").performClick()
        waitForText("Gone Habit")
        rule.onNodeWithText("Kept Habit").assertDoesNotExist()
        rule.onNodeWithContentDescription("Gone Habit").performClick()
        rule.onNodeWithText("Remove Habit(s)").performClick()
        waitForText("Remove this habit?")
        rule.onNodeWithText("Keep stats").performClick()
        rule.onNodeWithText("Keep stats").assertIsOff()
        rule.onNodeWithText("Its history is deleted for good. This can't be undone.").assertIsDisplayed()
        rule.onNodeWithText("Remove").performClick()
        waitForText("ADD HABIT")
        waitFor { runBlocking { db.habitDao().getById(gone) } == null }
        assertTrue("completions cascaded", !isDone(gone))
    }

    @Test
    fun removeHabit_notStartedYet_saysThereIsNoHistoryToKeep() {
        val (id) = insert(habit("Scheduled", start = today.plusDays(3)))
        openFromMenu("REMOVE HABIT")
        waitForText("Scheduled")
        rule.onNodeWithContentDescription("Scheduled").performClick()
        rule.onNodeWithText("Remove Habit(s)").performClick()
        waitForText("Remove this habit?")
        rule.onNodeWithText("It hasn't started yet, so there is no history to keep.")
            .assertIsDisplayed()
        rule.onNodeWithText("Remove").performClick()
        waitForText("ADD HABIT")
        waitFor { runBlocking { db.habitDao().getById(id) } == null }

        // So Settings has no phantom "deleted habit with stats" to count.
        rule.onNodeWithText("SETTINGS").performClick()
        waitForText("No deleted habits have stats kept.")
    }

    @Test
    fun removeHabit_selectionSurvivesRotation() {
        insert(habit("One"), habit("Two"))
        openFromMenu("REMOVE HABIT")
        waitForText("One")
        rule.onNodeWithContentDescription("Two").performClick()
        rule.activityRule.scenario.recreate()
        waitForText("1 selected")
        rule.onNodeWithContentDescription("Two").assertIsOn()
        rule.onNodeWithContentDescription("One").assertIsOff()
    }

    // ---- Edit Habit ------------------------------------------------------------------------

    @Test
    fun editHabit_saveOnlyRealChanges_discardAsksFirst() {
        val (id) = insert(habit("Editable", color = "coral"))
        openFromMenu("EDIT HABIT")
        waitForText("Editable")
        rule.onNodeWithText("Editable").performClick()
        waitForText("Save")
        waitForCrumbs("Home", "Menu", "Edit Habit", "Edit")
        rule.onNodeWithText("Save").assertIsNotEnabled()

        // Back with no change leaves at once.
        Espresso.pressBack()
        waitForText("Editable")
        rule.onNodeWithText("Save").assertDoesNotExist()

        // Change the colour; header back asks; keep editing; save.
        rule.onNodeWithText("Editable").performClick()
        waitForText("Save")
        rule.onNodeWithContentDescription("Green").performScrollTo().performClick()
        rule.onNodeWithText("Save").assertIsEnabled()
        rule.onNodeWithContentDescription("Back").performClick()
        waitForText("Discard changes?")
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithText("Save").performClick()
        waitFor { runBlocking { db.habitDao().getById(id)!!.colorKey } == "green" }

        // Rename, then system back, then discard: nothing saved.
        waitForText("Editable")
        rule.onNodeWithText("Editable").performClick()
        waitForText("Save")
        rule.onNodeWithText("Editable").performTextInput(" twice")
        Espresso.closeSoftKeyboard()
        Espresso.pressBack()
        waitForText("Discard changes?")
        rule.onNodeWithText("Discard").performClick()
        waitForText("Editable")
        assertEquals("Editable", runBlocking { db.habitDao().getById(id)!!.name })
    }

    // ---- Statistics ------------------------------------------------------------------------

    @Test
    fun statistics_listSectionsAndNumbers() {
        val (active, deleted, _) = insert(
            habit("Tracked", start = today.minusDays(4)),
            habit("Retired", start = today.minusDays(10)),
            habit("Not Yet", start = today.plusDays(2)),
        )
        runBlocking {
            // Tracked: done 2, 3 and 4 days ago and today → 4/5 due once today counts, streak 3+1.
            listOf(4L, 3L, 1L, 0L).forEach {
                db.completionDao().markDone(Completion(active, today.minusDays(it)))
            }
            db.habitDao().remove(listOf(deleted), keepStats = true, today = today.minusDays(2))
        }

        openFromMenu("STATISTICS")
        waitForText("Tracked")
        rule.onNodeWithText("Deleted").assertIsDisplayed()
        rule.onNodeWithText("Retired").assertIsDisplayed()
        rule.onNodeWithText("Not Yet").assertDoesNotExist()
        assertTrue(top("Tracked") < top("Deleted"))
        assertTrue(top("Deleted") < top("Retired"))

        rule.onNodeWithText("Tracked").performClick()
        waitForCrumbs("Home", "Menu", "Statistics", "Stats")
        rule.onNodeWithText("Days done").assertDoesNotExist()      // collapsed by default
        rule.onNodeWithText("Tracked").performClick()               // the strip
        waitForText("Days done")
        // Due: 4 days ago .. today = 5; done 4, 3, 1, 0 days ago = 4. Missed 2 days ago.
        rule.onNodeWithText("4/5").assertIsDisplayed()
        // Current: yesterday + today. Longest: also 2 (4–3 days ago, or yesterday + today).
        rule.onAllNodesWithText("2 days").assertCountEquals(2)
    }

    @Test
    fun editingAHabit_leavesItsStatisticsExactlyAsTheyWere() {
        // 21 due days (20 days ago .. today), missed 15, 9 and 4 days ago, today done:
        // 18/21, current streak 4 (3 days ago .. today), longest 5.
        val (id) = insert(
            habit("Before Edit", start = today.minusDays(20), color = "coral", icon = "dumbbell"),
        )
        val missed = setOf(15L, 9L, 4L)
        runBlocking {
            (0L..20L).filter { it !in missed }.forEach {
                db.completionDao().markDone(Completion(id, today.minusDays(it)))
            }
        }

        fun openStatsFor(name: String) {
            openFromMenu("STATISTICS")
            waitForText(name)
            rule.onNodeWithText(name).performClick()
            waitForCrumbs("Home", "Menu", "Statistics", "Stats")
            rule.onNodeWithText(name).performClick()           // expand the strip
            waitForText("Days done")
        }

        /** Everything Statistics shows for the habit: the strip's numbers and the calendar. */
        fun snapshot(): List<String> {
            val numbers = listOf("18/21", "4 days", "5 days").map { text ->
                "$text×" + rule.onAllNodesWithText(text).fetchSemanticsNodes().size
            }
            val days = listOf("done", "missed", "upcoming").map { status ->
                "$status×" + rule.onAllNodes(hasContentDescription(", $status", substring = true))
                    .fetchSemanticsNodes().size
            }
            return numbers + days
        }

        fun backToHome() {
            repeat(3) {
                rule.onNodeWithContentDescription("Back").performClick()
                rule.waitForIdle()
            }
            rule.waitUntilAtLeastOneExists(hasContentDescription("Menu"))
        }

        openStatsFor("Before Edit")
        val before = snapshot()
        assertTrue("numbers are on screen: $before", before.take(3).none { it.endsWith("×0") })
        backToHome()

        // Edit every editable field.
        openFromMenu("EDIT HABIT")
        waitForText("Before Edit")
        rule.onNodeWithText("Before Edit").performClick()
        waitForText("Save")
        rule.onNodeWithText("Before Edit").performTextClearance()
        rule.onNodeWithText("Habit name").performTextInput("after edit")
        rule.onNodeWithText("About Before Edit.").performTextClearance()
        rule.onNodeWithText("Description").performTextInput("Changed.")
        rule.onNodeWithContentDescription("Green").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Coffee").performScrollTo().performClick()
        rule.onNodeWithText("Save").performClick()
        waitForText("After Edit")                               // back on the Edit list
        rule.onNodeWithContentDescription("Back").performClick()
        waitForText("ADD HABIT")
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Menu"))

        openStatsFor("After Edit")
        assertEquals(before, snapshot())

        val saved = runBlocking { db.habitDao().getById(id)!! }
        assertEquals("After Edit", saved.name)
        assertEquals("green", saved.colorKey)
        assertEquals("coffee", saved.iconKey)
        assertEquals(today.minusDays(20), saved.startDate)
    }

    @Test
    fun statistics_monthShownSurvivesRotation() {
        insert(habit("Paged", start = today.minusMonths(2).withDayOfMonth(1)))
        openFromMenu("STATISTICS")
        waitForText("Paged")
        rule.onNodeWithText("Paged").performClick()
        rule.waitUntilAtLeastOneExists(hasContentDescription("Previous month"))
        rule.onNodeWithContentDescription("Previous month").performClick()
        val previous = java.time.YearMonth.from(today).minusMonths(1)
        val label = previous.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
        waitForText(label)
        rule.activityRule.scenario.recreate()
        waitForText(label)
    }

    // ---- Settings --------------------------------------------------------------------------

    @Test
    fun settings_themeIsSaved() {
        openFromMenu("SETTINGS")
        waitForText("Tokyo Night")
        rule.onNodeWithText("Tokyo Night").performClick()
        waitFor { runBlocking { db.appSettingsDao().get()?.themeMode } == ThemeMode.TOKYO_NIGHT }
        rule.onNodeWithText("Light").performClick()
        waitFor { runBlocking { db.appSettingsDao().get()?.themeMode } == ThemeMode.LIGHT }
    }

    @Test
    fun settings_clearDeletedStats_asksThenPurgesOnlyDeleted() {
        val (keep, purge) = insert(habit("Still Here"), habit("Old One"))
        runBlocking {
            db.completionDao().markDone(Completion(purge, today))
            db.habitDao().remove(listOf(purge), keepStats = true, today = today)
        }
        openFromMenu("SETTINGS")
        waitForText("1 deleted habit still has its stats kept.")
        rule.onNodeWithText("Clear deleted stats").performClick()
        waitForText("Clear deleted stats?")
        rule.onNodeWithText("Cancel").performClick()
        assertTrue(runBlocking { db.habitDao().getById(purge) } != null)

        rule.onNodeWithText("Clear deleted stats").performClick()
        waitForText("Clear deleted stats?")
        rule.onNodeWithText("Clear").performClick()
        waitForText("No deleted habits have stats kept.")
        assertNull(runBlocking { db.habitDao().getById(purge) })
        assertTrue(runBlocking { db.habitDao().getById(keep) } != null)
    }
}
