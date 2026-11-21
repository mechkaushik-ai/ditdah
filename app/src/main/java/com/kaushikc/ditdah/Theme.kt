package com.kaushikc.ditdah

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Amber = Color(0xFFFFB703)
private val DeepAmber = Color(0xFF8A5A00)
private val Navy = Color(0xFF14213D)

private val LightColors = lightColorScheme(
    primary = DeepAmber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB0),
    onPrimaryContainer = Color(0xFF2B1700),
    secondary = Navy,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF2B1700),
    primaryContainer = Color(0xFF5E4100),
    onPrimaryContainer = Color(0xFFFFDDB0),
    secondary = Color(0xFFB9C6EA),
    background = Color(0xFF0E1628),
    surface = Color(0xFF0E1628),
)

/** Uses the phone's wallpaper colours on Android 12+, and amber-on-navy otherwise. */
@Composable
fun DitDahTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
