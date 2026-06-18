package com.focuss.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focuss.data.ThemeMode

/** A small uppercase section label, e.g. "ACTIVE DAYS". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** Cycles System → Light → Dark; the icon shows the current mode. */
@Composable
fun ThemeToggleButton(mode: ThemeMode, onCycle: () -> Unit) {
    FilledTonalIconButton(onClick = onCycle) {
        Icon(
            imageVector = when (mode) {
                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                ThemeMode.DARK -> Icons.Rounded.DarkMode
            },
            contentDescription = "Theme: ${mode.name.lowercase()}",
        )
    }
}
