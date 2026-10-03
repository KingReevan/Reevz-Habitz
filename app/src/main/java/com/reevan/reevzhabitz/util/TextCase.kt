package com.reevan.reevzhabitz.util

/**
 * Capitalises the first letter of every word, leaving every other character exactly as typed:
 * "drink water" → "Drink Water", "read 20 pages" → "Read 20 Pages", "learn iOS" → "Learn IOS".
 *
 * A word starts at the beginning of the text or after whitespace. The result is always the same
 * length as the input — each letter maps to one title-case character — so a text field's cursor
 * and selection stay valid when this runs on every keystroke.
 */
fun capitalizeWords(text: String): String {
    val out = StringBuilder(text.length)
    var atWordStart = true
    for (char in text) {
        out.append(if (atWordStart && char.isLetter()) char.titlecaseChar() else char)
        atWordStart = char.isWhitespace()
    }
    return out.toString()
}
