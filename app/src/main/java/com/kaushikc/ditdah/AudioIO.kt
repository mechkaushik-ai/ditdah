package com.kaushikc.ditdah

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * The beep you hear while holding the tap key, with no noticeable delay.
 *
 * A background thread keeps a low-latency audio stream running and fades the
 * tone in and out over 4 ms whenever the key goes down or up, so it starts
 * instantly and never clicks.
 */
class Sidetone {
    @Volatile var frequencyHz: Int = 650
    @Volatile var volume: Float = 0.8f
    @Volatile private var keyDown = false
    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ loop() }, "DitDah-sidetone").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        running = false
        keyDown = false
        thread?.join(500)
        thread = null
    }

    fun key(down: Boolean) {
        keyDown = down
    }

    private fun loop() {
        val rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC).takeIf { it > 0 } ?: 48000
        val minBuffer = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setBufferSizeInBytes(max(minBuffer, CHUNK * 4))
                .build()
        } catch (e: Exception) {
            running = false
            return
        }

        val chunk = ShortArray(CHUNK)
        val rampStep = 1.0 / (0.004 * rate)
        var gain = 0.0
        var phase = 0.0
        try {
            track.play()
            while (running) {
                val target = if (keyDown) volume.toDouble() else 0.0
                val step = 2 * PI * frequencyHz / rate
                for (i in 0 until CHUNK) {
                    gain += (target - gain).coerceIn(-rampStep, rampStep)
                    chunk[i] = (sin(phase) * gain * 0.7 * Short.MAX_VALUE).toInt().toShort()
                    phase += step
                    if (phase > 2 * PI) phase -= 2 * PI
                }
                track.write(chunk, 0, CHUNK)
            }
        } finally {
            try {
                track.stop()
            } catch (e: Exception) {
                // Already stopped.
            }
            track.release()
        }
    }

    private companion object {
        /** ~2.7 ms of audio per write keeps the key responsive. */
        const val CHUNK = 128
    }
}

/** What the microphone hears right now, for the on-screen indicator. */
data class ListenStatus(
    val toneOn: Boolean = false,
    val level: Float = 0f,
    val frequencyHz: Int = 0,
    val wpm: Int = 0,
)

/**
 * Listens to the microphone and decodes Morse tones into written Morse.
 * Audio is analysed in memory as it arrives and is never recorded or stored.
 */
class MicListener {

    /**
     * Runs until the calling coroutine is cancelled. [onText] receives decoded
     * Morse such as "... " (a letter) or "/ " (a word gap).
     * Throws [SecurityException] without microphone permission, or
     * [IllegalStateException] if the microphone can't be opened.
     */
    @SuppressLint("MissingPermission") // The screen asks for permission before calling this.
    suspend fun listen(
        startWpm: Int,
        onStatus: (ListenStatus) -> Unit,
        onText: (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val rate = 44100 // The one rate every Android microphone supports.
        val minBuffer = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minBuffer > 0) { "Microphone not available" }

        val record = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION) // Least processing, keeps steady tones.
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(max(minBuffer, rate / 5 * 2))
            .build()
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("Microphone not available")
        }

        val detector = MorseToneDetector(rate)
        val decoder = MorseTimingDecoder(startWpm)
        val buffer = ShortArray(rate / 100)
        var sinceStatus = 0
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                val n = record.read(buffer, 0, buffer.size)
                if (n <= 0) continue
                detector.process(buffer, n) { reading ->
                    decoder.step(reading.on, reading.hopMs, onText)
                    // Refresh the on-screen indicator about 20 times a second.
                    if (++sinceStatus >= 5) {
                        sinceStatus = 0
                        onStatus(ListenStatus(reading.on, reading.level, reading.frequencyHz, decoder.wpm))
                    }
                }
            }
        } finally {
            decoder.finish(onText)
            try {
                record.stop()
            } catch (e: Exception) {
                // Already stopped.
            }
            record.release()
            onStatus(ListenStatus())
        }
    }
}
