package com.neosharks.focuss.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.neosharks.focuss.data.ThemeMode

// Branded fallback scheme (used on Android < 12, where Material You isn't available).
private val FallbackLight = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Indigo,
    tertiary = Green,
    error = Red,
)

private val FallbackDark = darkColorScheme(
    primary = IndigoDark,
    onPrimary = Color.White,
    secondary = IndigoDark,
    tertiary = GreenDark,
    error = RedDark,
)

/** Resolve a stored [ThemeMode] into an actual light/dark choice. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/**
 * App theme. Defaults to the device theme and uses **Material You dynamic color**
 * on Android 12+, so the palette (primary / secondary / tertiary / containers) is
 * derived from the user's wallpaper — multiple system-sourced colors, minimal and
 * personal. Falls back to a branded indigo scheme on older devices.
 */
@Composable
fun FocussTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    val context = LocalContext.current

    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> FallbackDark
        else -> FallbackLight
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = FocussTypography,
        content = content,
    )
}
