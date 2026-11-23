package com.kaushikc.ditdah

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------- Send

@Composable
fun SendScreen(
    text: String,
    onTextChange: (String) -> Unit,
    isPlaying: Boolean,
    activeChar: Int,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    controls: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val morse = remember(text) { MorseCode.encode(text) }
    val unsupported = remember(text) { MorseCode.unsupported(text) }
    val highlightBg = MaterialTheme.colorScheme.primary
    val highlightFg = MaterialTheme.colorScheme.onPrimary

    // The Morse version of the message, with the character being played highlighted.
    val preview = remember(text, activeChar, highlightBg, highlightFg) {
        buildAnnotatedString {
            var first = true
            var pendingWordGap = false
            text.forEachIndexed { i, ch ->
                if (ch.isWhitespace()) {
                    if (!first) pendingWordGap = true
                    return@forEachIndexed
                }
                val code = MorseCode.codeFor(ch) ?: return@forEachIndexed
                if (!first) append(if (pendingWordGap) "  /  " else "   ")
                first = false
                pendingWordGap = false
                if (i == activeChar) {
                    withStyle(
                        SpanStyle(
                            background = highlightBg,
                            color = highlightFg,
                            fontWeight = FontWeight.Bold
                        )
                    ) { append(code) }
                } else {
                    append(code)
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Your message") },
            supportingText = { Text("${text.length} / $MAX_CHARS") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        if (unsupported.isNotEmpty()) {
            Text(
                "Morse has no code for: ${unsupported.joinToString(" ")} (these will be skipped)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Morse code",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    if (activeChar in text.indices) {
                        Text(
                            text[activeChar].uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (preview.isEmpty()) buildAnnotatedString { append("Type something above") } else preview,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    lineHeight = 30.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isPlaying) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = onPlay,
                    enabled = morse.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play")
                }
            }
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, morse)
                    context.startActivity(Intent.createChooser(send, "Share Morse code"))
                },
                enabled = morse.isNotEmpty()
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Share")
            }
        }

        controls()
    }
}

// ---------------------------------------------------------------- Shared controls

@Composable
fun PlaybackControls(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    spacingWpm: Int,
    onSpacingChange: (Int) -> Unit,
    pitch: Int,
    onPitchChange: (Int) -> Unit,
    volume: Int,
    onVolumeChange: (Int) -> Unit,
    sound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    flash: Boolean,
    onFlashChange: (Boolean) -> Unit,
    hasFlash: Boolean,
    vibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
    hasVibrator: Boolean,
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Output", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = sound,
                    onClick = { onSoundChange(!sound) },
                    label = { Text("Sound") }
                )
                FilterChip(
                    selected = flash && hasFlash,
                    onClick = { onFlashChange(!flash) },
                    enabled = hasFlash,
                    label = { Text("Flashlight") }
                )
                FilterChip(
                    selected = vibrate && hasVibrator,
                    onClick = { onVibrateChange(!vibrate) },
                    enabled = hasVibrator,
                    label = { Text("Vibrate") }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Speed: $wpm words per minute", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = wpm.toFloat(),
                onValueChange = { onWpmChange(it.roundToInt()) },
                valueRange = 5f..40f,
                steps = 34
            )

            if (wpm > 5) {
                Text(
                    if (spacingWpm < wpm) {
                        "Spacing: $spacingWpm WPM (extra gaps between letters, for learning)"
                    } else {
                        "Spacing: normal"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = spacingWpm.toFloat(),
                    onValueChange = { onSpacingChange(it.roundToInt()) },
                    valueRange = 5f..wpm.toFloat(),
                    steps = (wpm - 6).coerceAtLeast(0)
                )
            }

            Text("Tone: $pitch Hz", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = pitch.toFloat(),
                onValueChange = { onPitchChange((it / 10f).roundToInt() * 10) },
                valueRange = 400f..1000f,
                steps = 59
            )

            Text("Volume: $volume%", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = volume.toFloat(),
                onValueChange = { onVolumeChange((it / 5f).roundToInt() * 5) },
                valueRange = 10f..100f,
                steps = 17
            )
        }
    }
}

// ---------------------------------------------------------------- Decode

@Composable
fun DecodeScreen(
    morse: String,
    onMorseChange: (String) -> Unit,
    wpm: Int,
    pitch: Int,
    volume: Int,
    isPlaying: Boolean,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    controls: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val decoded = remember(morse) { MorseCode.decode(morse) }
    val scope = rememberCoroutineScope()

    // The tap key and microphone run longer than one screen update, so they
    // must always read the latest text and settings rather than stale copies.
    val currentMorse by rememberUpdatedState(morse)
    val currentOnChange by rememberUpdatedState(onMorseChange)
    val currentWpm by rememberUpdatedState(wpm)

    fun appendSymbol(symbol: Char) = currentOnChange((currentMorse + symbol).take(MAX_MORSE))
    fun letterGap() {
        val m = currentMorse
        if (m.isNotEmpty() && !m.endsWith(" ")) currentOnChange("$m ")
    }
    fun wordGap() {
        val m = currentMorse.trimEnd()
        if (m.isNotEmpty() && !m.endsWith("/")) currentOnChange("$m / ")
    }
    /** Adds decoded microphone text, keeping one space between letters. */
    fun appendHeard(text: String) {
        val m = currentMorse
        val joined = when {
            text == "/ " -> if (m.isBlank() || m.trimEnd().endsWith("/")) m else m.trimEnd() + " / "
            m.isEmpty() || m.endsWith(" ") -> m + text
            else -> "$m $text"
        }
        currentOnChange(joined.take(MAX_MORSE))
    }

    // Instant beep while the tap key is held.
    val sidetone = remember { Sidetone() }
    sidetone.frequencyHz = pitch
    sidetone.volume = volume / 100f
    // Runs only while this screen is open and the app is in the foreground.
    LifecycleResumeEffect(Unit) {
        sidetone.start()
        onPauseOrDispose { sidetone.stop() }
    }

    var keyDown by remember { mutableStateOf(false) }
    var gapJob by remember { mutableStateOf<Job?>(null) }
    val unit = MorseCode.unitMs(wpm)

    // Microphone decoding.
    val listener = remember { MicListener() }
    var listenJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf(ListenStatus()) }
    var micError by remember { mutableStateOf<String?>(null) }
    val listening = listenJob != null

    fun startListening() {
        micError = null
        onStop()
        val job = scope.launch {
            try {
                listener.listen(
                    startWpm = currentWpm,
                    onStatus = { status = it },
                    onText = { text -> scope.launch { appendHeard(text) } },
                )
            } catch (e: SecurityException) {
                micError = "Microphone permission is needed to listen."
            } catch (e: CancellationException) {
                throw e // Normal stop, not an error.
            } catch (e: Exception) {
                micError = "The microphone is busy or unavailable."
            }
        }
        listenJob = job
        job.invokeOnCompletion { if (listenJob === job) listenJob = null }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else micError = "Microphone permission was declined."
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stopListening() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = morse,
            onValueChange = { onMorseChange(it.take(MAX_MORSE)) },
            label = { Text("Morse code") },
            supportingText = { Text("Use . and -   Space between letters   / between words") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton("·", Modifier.weight(1f)) { appendSymbol('.') }
            KeyButton("−", Modifier.weight(1f)) { appendSymbol('-') }
            KeyButton("Gap", Modifier.weight(1f)) { letterGap() }
            KeyButton("/", Modifier.weight(1f)) { wordGap() }
            KeyButton("⌫", Modifier.weight(1f)) { currentOnChange(currentMorse.dropLast(1)) }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Text", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    decoded.ifEmpty { "Decoded text appears here" },
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isPlaying) {
                        OutlinedButton(onClick = onStop) { Text("Stop") }
                    } else {
                        OutlinedButton(
                            onClick = {
                                stopListening()
                                onPlay(decoded)
                            },
                            enabled = decoded.isNotBlank()
                        ) { Text("Play it back") }
                    }
                    OutlinedButton(
                        onClick = { onMorseChange("") },
                        enabled = morse.isNotEmpty()
                    ) { Text("Clear") }
                }
            }
        }

        // ---- Listen through the microphone
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Listen", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Hold your phone near a Morse sound (400–1000 Hz). Set Speed below close " +
                        "to the sender's speed; the app then follows it automatically.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (listening) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(
                                    if (status.toneOn) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (status.frequencyHz > 0) {
                                "Tone ~${status.frequencyHz} Hz   Sender ~${status.wpm} WPM"
                            } else {
                                "Listening…"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    LinearProgressIndicator(
                        progress = { status.level },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                micError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (listening) {
                    Button(
                        onClick = { stopListening() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) { Text("Stop listening") }
                } else {
                    Button(onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) startListening()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }) { Text("Start listening") }
                }
            }
        }

        // ---- Tap key
        Text("Tap key", style = MaterialTheme.typography.titleMedium)
        Text(
            "Press briefly for a dot, hold for a dash; you'll hear each one as you key it. " +
                "Pause to finish a letter, and pause longer to start a new word.",
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(140.dp)
                .background(
                    if (keyDown) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                )
                .pointerInput(wpm) {
                    detectTapGestures(
                        onPress = {
                            gapJob?.cancel()
                            keyDown = true
                            sidetone.key(true)
                            val start = System.currentTimeMillis()
                            tryAwaitRelease()
                            sidetone.key(false)
                            keyDown = false
                            val held = System.currentTimeMillis() - start
                            appendSymbol(if (held < 2 * unit) '.' else '-')
                            gapJob = scope.launch {
                                delay((4 * unit).toLong())
                                letterGap()
                                delay((6 * unit).toLong())
                                wordGap()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "TAP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (keyDown) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // ---- Speed, spacing, tone and volume (shared with the Send screen)
        controls()
    }
}

private const val MAX_MORSE = 1500

@Composable
private fun KeyButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ---------------------------------------------------------------- Chart

@Composable
fun ChartScreen(onPlay: (String) -> Unit) {
    val entries = remember { MorseCode.table.entries.toList() }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Tap any character to hear it.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp)
            )
        }
        items(entries, key = { it.key.toString() }) { (ch, code) ->
            ElevatedCard(onClick = { onPlay(ch.toString()) }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        ch.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(code, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                }
            }
        }
    }
}
