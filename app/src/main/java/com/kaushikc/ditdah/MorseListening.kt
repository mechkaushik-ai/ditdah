package com.kaushikc.ditdah

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Finds a Morse tone in microphone audio.
 *
 * Every 10 ms it measures the strongest tone between 400 and 1000 Hz (Goertzel
 * filters over a 20 ms window) and decides "tone on" or "tone off" against a
 * self-adjusting noise floor, so it works in quiet and noisy rooms without setup.
 * Pure Kotlin (no Android code), so it can be unit tested.
 */
class MorseToneDetector(private val sampleRate: Int = 44100) {

    data class Reading(
        val on: Boolean,
        val frequencyHz: Int,
        /** 0..1, how far the current level sits above the noise floor. */
        val level: Float,
        val hopMs: Double,
    )

    private val hop = sampleRate / 100          // 10 ms
    private val window = hop * 2                // 20 ms
    private val frequencies = (400..1000 step 25).toList()
    private val coefficients = DoubleArray(frequencies.size) { 2 * cos(2 * PI * frequencies[it] / sampleRate) }
    private val hann = DoubleArray(window) { 0.5 - 0.5 * cos(2 * PI * it / (window - 1)) }

    private val ring = DoubleArray(window)
    private var filled = 0
    private var writePos = 0
    private var sinceHop = 0
    private val frame = DoubleArray(window)

    private var noiseDb = Double.NaN
    private var peakDb = Double.NaN
    private var on = false

    val hopMs: Double get() = hop * 1000.0 / sampleRate

    /** Feeds raw 16-bit samples; calls [onReading] once per 10 ms of audio. */
    fun process(samples: ShortArray, count: Int = samples.size, onReading: (Reading) -> Unit) {
        for (i in 0 until count) {
            // Circular buffer: writePos always points at the oldest sample.
            ring[writePos] = samples[i] / 32768.0
            writePos = (writePos + 1) % window
            if (filled < window) filled++
            sinceHop++
            if (sinceHop >= hop && filled == window) {
                sinceHop = 0
                onReading(analyse())
            }
        }
    }

    private fun analyse(): Reading {
        for (i in 0 until window) frame[i] = ring[(writePos + i) % window] * hann[i]
        var best = 0.0
        var bestIndex = 0
        for (k in coefficients.indices) {
            val c = coefficients[k]
            var q1 = 0.0
            var q2 = 0.0
            for (v in frame) {
                val q0 = c * q1 - q2 + v
                q2 = q1
                q1 = q0
            }
            val power = q1 * q1 + q2 * q2 - c * q1 * q2
            if (power > best) {
                best = power
                bestIndex = k
            }
        }
        val db = 10 * log10(best / (window.toDouble() * window) + 1e-12)

        if (noiseDb.isNaN()) {
            noiseDb = db
            peakDb = db
        }
        // Noise floor drops instantly and rises slowly; the peak does the opposite.
        noiseDb = if (db < noiseDb) db else noiseDb + 0.02 * (db - noiseDb)
        peakDb = if (db > peakDb) db else peakDb + 0.002 * (db - peakDb)

        val span = peakDb - noiseDb
        on = if (span < MIN_SPAN_DB) {
            false
        } else {
            val high = noiseDb + 0.55 * span
            val low = noiseDb + 0.40 * span
            if (on) db > low else db > high
        }
        val level = ((db - noiseDb) / max(span, MIN_SPAN_DB)).coerceIn(0.0, 1.0).toFloat()
        return Reading(on, frequencies[bestIndex], level, hopMs)
    }

    private companion object {
        /** A tone must stand at least this far above the noise to count. */
        const val MIN_SPAN_DB = 12.0
    }
}

/**
 * Turns a stream of tone-on / tone-off timings into written Morse
 * ("... --- ... / "), learning the sender's speed as it goes.
 *
 * Dots and dashes are separated using each letter's own timing where possible
 * (a dash is about 3 times a dot), otherwise the current speed estimate.
 * Pure Kotlin (no Android code), so it can be unit tested.
 */
class MorseTimingDecoder(startWpm: Int) {

    private var dotMs = MorseCode.unitMs(startWpm)
    private var state = false
    private var durationMs = 0.0
    private val tones = ArrayList<Double>()

    /** 0 = inside a letter, 1 = letter written, 2 = word gap written (or idle). */
    private var stage = 2

    /** The sender's speed as currently estimated. */
    val wpm: Int get() = (1200.0 / dotMs).toInt()

    /** Advances time by [ms] with the tone [on] or off; [emit] receives finished text. */
    fun step(on: Boolean, ms: Double, emit: (String) -> Unit) {
        if (on == state) {
            durationMs += ms
            if (!on) checkGap(emit)
            return
        }
        if (state) {
            // A tone just ended. Ignore clicks shorter than a third of a dot.
            if (durationMs >= max(15.0, 0.3 * dotMs)) tones += max(5.0, durationMs - SMEAR_MS)
        } else if (tones.isNotEmpty() && stage == 0) {
            // A short gap inside a letter is one unit long: a free speed sample.
            dotMs += 0.1 * (durationMs + SMEAR_MS - dotMs)
            clampDot()
        }
        state = on
        durationMs = ms
        if (on) stage = 0
    }

    /** Writes out any letter still being received (call when listening stops). */
    fun finish(emit: (String) -> Unit) {
        if (tones.isNotEmpty()) {
            emit(classify() + " ")
            tones.clear()
        }
        stage = 2
    }

    private fun checkGap(emit: (String) -> Unit) {
        val gap = durationMs + SMEAR_MS
        if (stage == 0 && tones.isNotEmpty() && gap >= 2 * dotMs) {
            emit(classify() + " ")
            tones.clear()
            stage = 1
        }
        if (stage == 1 && gap >= 5 * dotMs) {
            emit("/ ")
            stage = 2
        }
    }

    private fun classify(): String {
        val shortest = tones.min()
        val longest = tones.max()
        val threshold = if (tones.size >= 2 && longest >= 2.2 * shortest) {
            sqrt(longest * shortest)
        } else {
            2 * dotMs
        }
        val code = StringBuilder()
        for (d in tones) {
            if (d < threshold) {
                code.append('.')
                dotMs += 0.25 * (d - dotMs)
            } else {
                code.append('-')
                dotMs += 0.25 * (d / 3 - dotMs)
            }
        }
        clampDot()
        return code.toString()
    }

    private fun clampDot() {
        dotMs = min(max(dotMs, 20.0), 400.0)
    }

    private companion object {
        /** The detector's 20 ms window makes tones read ~10 ms long and gaps ~10 ms short. */
        const val SMEAR_MS = 10.0
    }
}
