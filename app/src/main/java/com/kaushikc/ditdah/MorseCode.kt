package com.kaushikc.ditdah

/** What kind of moment in a Morse signal this is. */
enum class Kind { TONE, SYMBOL_GAP, LETTER_GAP, WORD_GAP }

/**
 * One step of a Morse signal, measured in "units" (one unit = the length of a dot).
 * [charIndex] points at the character of the original text this step belongs to,
 * or -1 for the silence between letters and words.
 */
data class Element(val kind: Kind, val units: Int, val charIndex: Int)

/** One step of a Morse signal with its real duration, ready to be played. */
data class Segment(val on: Boolean, val ms: Double, val charIndex: Int)

/**
 * International Morse Code (ITU-R M.1677-1) and the standard "PARIS" timing:
 * dot = 1 unit, dash = 3 units, gap inside a letter = 1 unit,
 * gap between letters = 3 units, gap between words = 7 units,
 * and one unit lasts 1200 / WPM milliseconds.
 */
object MorseCode {

    /** Shown in decoded text when a code doesn't match any character. */
    const val UNKNOWN = '□'

    val letters: Map<Char, String> = linkedMapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--.."
    )

    val digits: Map<Char, String> = linkedMapOf(
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
        '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
    )

    val punctuation: Map<Char, String> = linkedMapOf(
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", ':' to "---...",
        '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '"' to ".-..-.",
        '@' to ".--.-."
    )

    /** Every supported character, in chart order: letters, digits, punctuation. */
    val table: Map<Char, String> = letters + digits + punctuation

    private val reverse: Map<String, Char> = table.entries.associate { it.value to it.key }

    /** The Morse code for one character, or null if Morse has no code for it. */
    fun codeFor(c: Char): String? = table[c.uppercaseChar()]

    /** Characters in [text] (other than spaces) that cannot be sent in Morse. */
    fun unsupported(text: String): Set<Char> =
        text.filter { !it.isWhitespace() && codeFor(it) == null }.toSet()

    /** Text to written Morse: letters separated by a space, words by " / ". */
    fun encode(text: String): String =
        text.trim()
            .split(Regex("\\s+"))
            .map { word -> word.mapNotNull { codeFor(it) }.joinToString(" ") }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")

    /** Accepts the look-alike dots and dashes people paste from websites. */
    fun normalize(morse: String): String = buildString {
        for (c in morse) {
            append(
                when (c) {
                    '·', '•', '∙', '⋅' -> '.'
                    '−', '–', '—', '_' -> '-'
                    '|' -> '/'
                    else -> c
                }
            )
        }
    }

    /** Written Morse to text. Spaces separate letters; "/" separates words. */
    fun decode(morse: String): String =
        normalize(morse)
            .split('/')
            .map { word ->
                word.trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .joinToString("") { code -> (reverse[code] ?: UNKNOWN).toString() }
            }
            .filter { it.isNotEmpty() }
            .joinToString(" ")

    /** Breaks [text] into the dots, dashes and gaps that make up its Morse signal. */
    fun elements(text: String): List<Element> {
        val out = ArrayList<Element>()
        var started = false
        var wordBreak = false
        text.forEachIndexed { i, ch ->
            if (ch.isWhitespace()) {
                if (started) wordBreak = true
                return@forEachIndexed
            }
            val code = codeFor(ch) ?: return@forEachIndexed
            if (started) {
                out += if (wordBreak) Element(Kind.WORD_GAP, 7, -1) else Element(Kind.LETTER_GAP, 3, -1)
            }
            started = true
            wordBreak = false
            code.forEachIndexed { e, symbol ->
                if (e > 0) out += Element(Kind.SYMBOL_GAP, 1, i)
                out += Element(Kind.TONE, if (symbol == '.') 1 else 3, i)
            }
        }
        return out
    }

    /** Length of one unit (a dot) in milliseconds at [wpm] words per minute. */
    fun unitMs(wpm: Int): Double = 1200.0 / wpm

    /**
     * The timed signal for [text].
     *
     * [wpm] is the speed of each character. If [spacingWpm] is lower, the gaps
     * between letters and words are stretched (Farnsworth timing), which makes
     * Morse easier to learn by ear without slowing the characters down.
     */
    fun segments(text: String, wpm: Int, spacingWpm: Int = wpm): List<Segment> {
        val unit = unitMs(wpm)
        val effective = spacingWpm.coerceIn(1, wpm)
        val letterGap: Double
        val wordGap: Double
        if (effective < wpm) {
            // ARRL Farnsworth formula: total extra delay per standard word, in ms.
            val ta = (60.0 * wpm - 37.2 * effective) / (wpm * effective) * 1000.0
            letterGap = 3 * ta / 19
            wordGap = 7 * ta / 19
        } else {
            letterGap = 3 * unit
            wordGap = 7 * unit
        }
        return elements(text).map { e ->
            when (e.kind) {
                Kind.TONE -> Segment(true, e.units * unit, e.charIndex)
                Kind.SYMBOL_GAP -> Segment(false, unit, e.charIndex)
                Kind.LETTER_GAP -> Segment(false, letterGap, -1)
                Kind.WORD_GAP -> Segment(false, wordGap, -1)
            }
        }
    }
}
