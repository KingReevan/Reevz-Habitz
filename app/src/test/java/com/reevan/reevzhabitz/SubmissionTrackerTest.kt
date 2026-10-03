package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.ui.common.SubmissionTracker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionTrackerTest {

    @Test
    fun aSubmissionIsUnderWayUntilItsWriteCompletes() {
        val tracker = SubmissionTracker()
        tracker.begin("a")
        assertFalse(tracker.isFinished("a"))
        tracker.complete("a")
        assertTrue(tracker.isFinished("a"))
    }

    @Test
    fun submissionsAreTrackedIndependently() {
        val tracker = SubmissionTracker()
        tracker.begin("a")
        tracker.begin("b")
        tracker.complete("b")
        assertFalse(tracker.isFinished("a"))
        assertTrue(tracker.isFinished("b"))
    }

    @Test
    fun anUnknownTokenCountsAsFinished() {
        // Only possible after the process was killed mid-write and restored with a saved token:
        // the screen must move on rather than wait forever with its action disabled.
        assertTrue(SubmissionTracker().isFinished("restored-after-process-death"))
    }
}
