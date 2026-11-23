package com.kaushikc.ditdah

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

class MorseListeningTest {

    private val rate = 44100

    /** Builds what a microphone would hear: Morse tones plus background hiss. */
    private fun synth(text: String, wpm: Int, freq: Int, noise: Double, seed: Long): ShortArray {
        val random = Random(seed)
        val out = ArrayList<Double>()
        repeat(rate / 2) { out += 0.0 }
        for (s in MorseCode.segments(text, wpm)) {
            // Up to 10% human timing wobble on every element.
            val n = (s.ms * (1 + (random.nextDouble() - 0.5) * 0.2) * rate / 1000).roundToInt()
            for (j in 0 until n) out += if (s.on) 0.5 * sin(2 * PI * freq * j / rate) else 0.0
        }
        repeat(rate * 3 / 2) { out += 0.0 }
        return ShortArray(out.size) {
            ((out[it] + random.nextGaussian() * noise).coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
        }
    }

    private fun listen(audio: ShortArray, startWpm: Int): Pair<String, Int> {
        val detector = MorseToneDetector(rate)
        val decoder = MorseTimingDecoder(startWpm)
        val morse = StringBuilder()
        detector.process(audio) { decoder.step(it.on, it.hopMs) { text -> morse.append(text) } }
        decoder.finish { morse.append(it) }
        return MorseCode.decode(morse.toString()) to decoder.wpm
    }

    @Test
    fun decodesCleanSignal() {
        assertEquals("SOS", listen(synth("SOS", 15, 650, 0.02, 1), 15).first)
    }

    @Test
    fun decodesNoisySignalAtDifferentTones() {
        assertEquals("HELLO WORLD", listen(synth("HELLO WORLD", 20, 700, 0.05, 2), 18).first)
        assertEquals("PARIS 123", listen(synth("PARIS 123", 12, 800, 0.1, 3), 15).first)
        assertEquals("CQ DE VU2", listen(synth("CQ DE VU2", 22, 550, 0.1, 4), 20).first)
    }

    @Test
    fun learnsTheSendersSpeed() {
        val (text, wpm) = listen(synth("THE QUICK BROWN FOX", 25, 600, 0.05, 5), 20)
        assertEquals("THE QUICK BROWN FOX", text)
        assertTrue("estimated $wpm WPM", wpm in 21..29)
    }

    @Test
    fun silenceDecodesToNothing() {
        assertEquals("", listen(synth("", 15, 650, 0.05, 6), 15).first)
    }

    @Test
    fun timingDecoderSeparatesLettersAndWords() {
        val decoder = MorseTimingDecoder(20) // 60 ms dots
        val out = StringBuilder()
        fun run(on: Boolean, ms: Int) = repeat(ms / 10) { decoder.step(on, 10.0) { out.append(it) } }
        // "A" (.-), letter gap, "N" (-.), word gap, "E" (.)
        run(true, 70); run(false, 50); run(true, 190); run(false, 170)
        run(true, 190); run(false, 50); run(true, 70); run(false, 450)
        run(true, 70); run(false, 600)
        assertEquals(".- -. / . / ", out.toString())
    }
}
