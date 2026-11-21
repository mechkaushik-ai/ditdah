package com.kaushikc.ditdah

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Plays a Morse signal as sound, flashlight blinks and/or vibration, all in step.
 *
 * Sound is a pure sine tone generated in memory (no audio files), with a 5 ms
 * fade at each edge so the beeps don't click. Needs no internet and collects nothing.
 */
class MorsePlayer(context: Context) {

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)
        }
    } catch (e: Exception) {
        null
    }

    private val cameraManager: CameraManager? = context.getSystemService(CameraManager::class.java)

    private val torchId: String? = try {
        cameraManager?.let { cm ->
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }
    } catch (e: Exception) {
        null
    }

    val hasFlash: Boolean get() = torchId != null
    val hasVibrator: Boolean get() = vibrator?.hasVibrator() == true

    /**
     * Plays [segments] and calls [onChar] with the index of the character being
     * sent (-1 between letters and when finished). Cancel the coroutine to stop.
     */
    suspend fun play(
        segments: List<Segment>,
        frequencyHz: Int,
        sound: Boolean,
        flash: Boolean,
        vibrate: Boolean,
        onChar: (Int) -> Unit
    ) {
        if (segments.isEmpty()) return

        val ends = DoubleArray(segments.size)
        var total = 0.0
        segments.forEachIndexed { i, s ->
            total += s.ms
            ends[i] = total
        }
        val totalMs = total

        val useFlash = flash && hasFlash
        val useVibrate = vibrate && hasVibrator
        var track: AudioTrack? = null

        try {
            if (sound) {
                val pcm = buildPcm(segments, frequencyHz)
                if (pcm.isNotEmpty()) {
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .setBufferSizeInBytes(pcm.size * 2)
                        .build()
                    track.write(pcm, 0, pcm.size)
                }
            }

            if (useVibrate) vibrator?.vibrate(waveform(segments))
            track?.play()
            val startClock = SystemClock.elapsedRealtime()

            var index = 0
            var lastChar = Int.MIN_VALUE
            var lastOn: Boolean? = null
            while (true) {
                val clockMs = (SystemClock.elapsedRealtime() - startClock).toDouble()
                // Follow the audio itself when it's playing, so the highlight and
                // flashlight stay locked to what you hear.
                val now = track?.let { it.playbackHeadPosition * 1000.0 / SAMPLE_RATE } ?: clockMs
                if (now >= totalMs || clockMs > totalMs + 3000) break

                while (index < ends.size - 1 && now >= ends[index]) index++
                val segment = segments[index]
                if (segment.on != lastOn) {
                    if (useFlash) setTorch(segment.on)
                    lastOn = segment.on
                }
                if (segment.charIndex != lastChar) {
                    onChar(segment.charIndex)
                    lastChar = segment.charIndex
                }
                delay(4)
            }
        } finally {
            withContext(NonCancellable) {
                track?.let {
                    try {
                        it.stop()
                    } catch (e: Exception) {
                        // Already stopped.
                    }
                    it.release()
                }
                if (useVibrate) vibrator?.cancel()
                if (useFlash) setTorch(false)
                onChar(-1)
            }
        }
    }

    private fun setTorch(on: Boolean) {
        val id = torchId ?: return
        try {
            cameraManager?.setTorchMode(id, on)
        } catch (e: Exception) {
            // The camera may be in use by another app; just skip the flash.
        }
    }

    /** Vibration pattern: alternating off/on durations, starting with "off". */
    private fun waveform(segments: List<Segment>): VibrationEffect {
        val timings = ArrayList<Long>()
        var currentOn = false
        var elapsed = 0.0
        var lastBoundary = 0L
        for (s in segments) {
            if (s.on != currentOn) {
                val boundary = elapsed.roundToLong()
                timings += boundary - lastBoundary
                lastBoundary = boundary
                currentOn = s.on
            }
            elapsed += s.ms
        }
        timings += elapsed.roundToLong() - lastBoundary
        return VibrationEffect.createWaveform(timings.toLongArray(), -1)
    }

    /** The whole message as one block of 16-bit audio samples. */
    private fun buildPcm(segments: List<Segment>, frequencyHz: Int): ShortArray {
        val counts = IntArray(segments.size)
        var elapsed = 0.0
        var previousEnd = 0
        segments.forEachIndexed { i, s ->
            elapsed += s.ms
            val end = (elapsed * SAMPLE_RATE / 1000.0).roundToInt()
            counts[i] = end - previousEnd
            previousEnd = end
        }

        val out = ShortArray(previousEnd)
        var pos = 0
        segments.forEachIndexed { i, s ->
            val n = counts[i]
            if (s.on) {
                val ramp = min(SAMPLE_RATE / 200, n / 2)
                for (j in 0 until n) {
                    val envelope = when {
                        ramp == 0 -> 1.0
                        j < ramp -> j.toDouble() / ramp
                        j >= n - ramp -> (n - 1 - j).toDouble() / ramp
                        else -> 1.0
                    }
                    val sample = sin(2 * PI * frequencyHz * j / SAMPLE_RATE) * envelope * AMPLITUDE
                    out[pos + j] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
            }
            pos += n
        }
        return out
    }

    companion object {
        const val SAMPLE_RATE = 44100
        private const val AMPLITUDE = 0.7
    }
}
