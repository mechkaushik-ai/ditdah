package com.kaushikc.ditdah

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Longest message the Send screen accepts. Keeps playback and memory sensible. */
const val MAX_CHARS = 300

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val shared = if (intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else {
            null
        }
        setContent {
            DitDahTheme {
                DitDahApp(initialText = shared ?: "Hello")
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    SEND("Send", Icons.AutoMirrored.Filled.Send),
    DECODE("Decode", Icons.Filled.Edit),
    CHART("Chart", Icons.AutoMirrored.Filled.List),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DitDahApp(initialText: String) {
    val context = LocalContext.current
    val player = remember { MorsePlayer(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf(initialText.take(MAX_CHARS)) }
    var morseInput by rememberSaveable { mutableStateOf("") }
    var wpm by rememberSaveable { mutableIntStateOf(15) }
    var spacingWpm by rememberSaveable { mutableIntStateOf(15) }
    var pitch by rememberSaveable { mutableIntStateOf(650) }
    var sound by rememberSaveable { mutableStateOf(true) }
    var flash by rememberSaveable { mutableStateOf(false) }
    var vibrate by rememberSaveable { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    var playJob by remember { mutableStateOf<Job?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var activeChar by remember { mutableIntStateOf(-1) }

    fun stop() {
        playJob?.cancel()
        playJob = null
        isPlaying = false
    }

    fun play(source: String) {
        stop()
        val segments = MorseCode.segments(source, wpm, spacingWpm)
        if (segments.isEmpty()) return
        isPlaying = true
        val job = scope.launch(Dispatchers.Default) {
            player.play(segments, pitch, sound, flash, vibrate) { activeChar = it }
        }
        playJob = job
        job.invokeOnCompletion {
            if (playJob === job) {
                isPlaying = false
                playJob = null
            }
        }
    }

    // Stop beeping and switch the torch off if the user leaves the app.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { stop() }

    val controls: @Composable () -> Unit = {
        PlaybackControls(
            wpm = wpm,
            onWpmChange = {
                wpm = it
                if (spacingWpm > it) spacingWpm = it
            },
            spacingWpm = spacingWpm,
            onSpacingChange = { spacingWpm = it },
            pitch = pitch,
            onPitchChange = { pitch = it },
            sound = sound,
            onSoundChange = { sound = it },
            flash = flash,
            onFlashChange = { flash = it },
            hasFlash = player.hasFlash,
            vibrate = vibrate,
            onVibrateChange = { vibrate = it },
            hasVibrator = player.hasVibrator,
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dit Dah") },
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = tabIndex == i,
                        onClick = {
                            if (tabIndex != i) stop()
                            tabIndex = i
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (Tab.entries[tabIndex]) {
                Tab.SEND -> SendScreen(
                    text = text,
                    onTextChange = { text = it.take(MAX_CHARS) },
                    isPlaying = isPlaying,
                    activeChar = activeChar,
                    onPlay = { play(text) },
                    onStop = { stop() },
                    controls = controls,
                )

                Tab.DECODE -> DecodeScreen(
                    morse = morseInput,
                    onMorseChange = { morseInput = it },
                    wpm = wpm,
                    isPlaying = isPlaying,
                    onPlay = { play(it) },
                    onStop = { stop() },
                )

                Tab.CHART -> ChartScreen(onPlay = { play(it) })
            }
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("OK") }
            },
            title = { Text("About Dit Dah") },
            text = {
                Text(
                    "Version ${BuildConfig.VERSION_NAME}\n\n" +
                        "Dit Dah turns your words into International Morse Code that you can " +
                        "hear, see and feel, and decodes Morse back into text.\n\n" +
                        "It works offline, has no ads, and collects no personal data.\n\n" +
                        "Made by Kaushik Chandrasekar in Bangalore."
                )
            }
        )
    }
}
