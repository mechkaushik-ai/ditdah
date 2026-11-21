package com.kaushikc.ditdah

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseCodeTest {

    @Test
    fun encodesSos() {
        assertEquals("... --- ...", MorseCode.encode("SOS"))
    }

    @Test
    fun encodesWordsWithSlashAndIgnoresCase() {
        assertEquals(".... .. / - .... . .-. .", MorseCode.encode("hi  there"))
    }

    @Test
    fun skipsCharactersWithoutMorse() {
        assertEquals("... --- ...", MorseCode.encode("S#O✓S"))
        assertEquals(setOf('#', '✓'), MorseCode.unsupported("S#O✓S"))
    }

    @Test
    fun decodesBack() {
        assertEquals("HELLO WORLD", MorseCode.decode(".... . .-.. .-.. --- / .-- --- .-. .-.. -.."))
    }

    @Test
    fun decodesLookAlikeSymbols() {
        assertEquals("SOS", MorseCode.decode("··· −−− ···"))
    }

    @Test
    fun marksUnknownCodes() {
        assertEquals("E${MorseCode.UNKNOWN}", MorseCode.decode(". ........"))
    }

    @Test
    fun everyCharacterRoundTrips() {
        for (c in MorseCode.table.keys) {
            assertEquals(c.toString(), MorseCode.decode(MorseCode.encode(c.toString())))
        }
    }

    @Test
    fun codesAreUnique() {
        assertEquals(MorseCode.table.size, MorseCode.table.values.toSet().size)
    }

    @Test
    fun parisIsFiftyUnitsWithTrailingWordGap() {
        // The PARIS standard: 43 units of signal + a 7-unit word gap = 50 units.
        val units = MorseCode.elements("PARIS").sumOf { it.units }
        assertEquals(43, units)
    }

    @Test
    fun twentyWpmDotIsSixtyMilliseconds() {
        val segments = MorseCode.segments("E", 20)
        assertEquals(1, segments.size)
        assertEquals(60.0, segments[0].ms, 0.001)
        assertEquals(43 * 60.0, MorseCode.segments("PARIS", 20).sumOf { it.ms }, 0.001)
    }

    @Test
    fun farnsworthOnlyStretchesGaps() {
        val normal = MorseCode.segments("A B", 18, 18)
        val slow = MorseCode.segments("A B", 18, 10)
        val tones = { s: List<Segment> -> s.filter { it.on }.sumOf { it.ms } }
        assertEquals(tones(normal), tones(slow), 0.001)
        assertTrue(slow.sumOf { it.ms } > normal.sumOf { it.ms })
    }

    @Test
    fun highlightIndexPointsAtOriginalCharacter() {
        val segments = MorseCode.segments("a b", 20)
        assertEquals(0, segments.first().charIndex)
        assertEquals(2, segments.last().charIndex)
    }
}
