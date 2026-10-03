package com.reevan.reevzhabitz

import com.reevan.reevzhabitz.util.capitalizeWords
import org.junit.Assert.assertEquals
import org.junit.Test

class TextCaseTest {

    @Test
    fun capitalisesTheFirstLetterOfEveryWord() {
        assertEquals("Drink Water", capitalizeWords("drink water"))
        assertEquals("Read 20 Pages", capitalizeWords("read 20 pages"))
    }

    @Test
    fun leavesTheRestOfEachWordAsTyped() {
        assertEquals("Learn IOS", capitalizeWords("learn iOS"))
        assertEquals("Self-care", capitalizeWords("self-care"))
        assertEquals("Don't Snooze", capitalizeWords("don't snooze"))
    }

    @Test
    fun aWordStartingWithADigitIsNotCapitalisedLater() {
        assertEquals("10km Run", capitalizeWords("10km run"))
    }

    @Test
    fun handlesRepeatedLeadingAndTrailingWhitespace() {
        assertEquals("  Two  Spaces ", capitalizeWords("  two  spaces "))
        assertEquals("Tab\tSeparated", capitalizeWords("tab\tseparated"))
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals("", capitalizeWords(""))
    }

    @Test
    fun keepsTheLengthSoTheCursorStaysPut() {
        // 'ß'.uppercase() is "SS" — two characters — which would shift the cursor. Title-casing
        // a single character never changes the length.
        listOf("ßtraße", "ǆungla", "émile zola", "ﬁx it").forEach {
            assertEquals(it, it.length, capitalizeWords(it).length)
        }
    }
}
