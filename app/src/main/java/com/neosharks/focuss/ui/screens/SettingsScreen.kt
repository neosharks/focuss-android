package com.neosharks.focuss.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neosharks.focuss.R
import com.neosharks.focuss.data.ThemeMode
import com.neosharks.focuss.ui.UiState
import com.neosharks.focuss.ui.components.SectionLabel

// Supported in-app languages: BCP-47 tag → endonym (shown in its own script).
// Empty tag = follow the system language.
private val LANGUAGES = listOf(
    "" to "",
    "en" to "English",
    "hi" to "हिन्दी",
    "ur" to "اردو",
    "de" to "Deutsch",
    "es" to "Español",
    "fr" to "Français",
    "ar" to "العربية",
    "zh" to "中文",
    "pt" to "Português",
    "ru" to "Русский",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onCycleTheme: () -> Unit,
    onSetLanguage: (String) -> Unit,
    onRequestBattery: () -> Unit,
    strictModeEnableIntent: () -> android.content.Intent,
    onStrictModeResult: () -> Unit,
    onDisableStrict: () -> Unit,
    onReviewPermissions: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val adminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { onStrictModeResult() }

    var pendingStrict by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }

    val langTag = state.appLanguage
    val langLabel = LANGUAGES.firstOrNull { it.first == langTag }
        ?.second?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.language_system)

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            SectionLabel(stringResource(R.string.appearance), modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 8.dp))
            SettingRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.theme),
                subtitle = stringResource(
                    when (state.themeMode) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    },
                ),
                onClick = onCycleTheme,
            )
            Spacer(Modifier.height(10.dp))
            SettingRow(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.language),
                subtitle = langLabel,
                onClick = { showLanguage = true },
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.reliability), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            SettingRow(
                icon = Icons.Rounded.BatteryChargingFull,
                title = stringResource(R.string.battery_optimisation),
                subtitle = stringResource(R.string.battery_optimisation_desc),
                trailing = {
                    if (state.batteryExempt) {
                        Text(stringResource(R.string.enabled), color = cs.tertiary, fontWeight = FontWeight.SemiBold)
                    } else {
                        TextButton(onClick = onRequestBattery) { Text(stringResource(R.string.allow)) }
                    }
                },
            )

            Spacer(Modifier.height(10.dp))
            SettingRow(
                icon = Icons.Rounded.Lock,
                title = stringResource(R.string.strict_mode),
                subtitle = stringResource(R.string.strict_mode_desc),
                trailing = {
                    Switch(
                        checked = state.strictMode,
                        onCheckedChange = { want ->
                            if (want) {
                                pendingStrict = true
                                adminLauncher.launch(strictModeEnableIntent())
                            } else {
                                onDisableStrict()
                            }
                        },
                    )
                },
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.permissions), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            SettingRow(
                icon = Icons.Rounded.Shield,
                title = stringResource(R.string.permissions),
                subtitle = stringResource(if (state.allPermissionsGranted) R.string.all_granted else R.string.setup_required),
                trailing = { TextButton(onClick = onReviewPermissions) { Text(stringResource(R.string.review)) } },
            )

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.about), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            val uriHandler = LocalUriHandler.current
            SettingRow(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.open_source_footer),
                subtitle = stringResource(R.string.view_on_github),
                onClick = { uriHandler.openUri("https://github.com/neosharks/focuss-android") },
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    // pendingStrict is consumed by the launcher result; kept for clarity/future use.
    if (pendingStrict && state.strictMode) pendingStrict = false

    if (showLanguage) {
        AlertDialog(
            onDismissRequest = { showLanguage = false },
            confirmButton = {},
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    LANGUAGES.forEach { (tag, endonym) ->
                        val label = if (tag.isEmpty()) stringResource(R.string.language_system) else endonym
                        val selected = tag == langTag
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguage = false
                                    if (tag != langTag) {
                                        onSetLanguage(tag)
                                        (context as? Activity)?.recreate()
                                    }
                                }
                                .padding(vertical = 12.dp),
                        ) {
                            androidx.compose.material3.RadioButton(selected = selected, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = cs.secondaryContainer, modifier = Modifier.size(40.dp)) {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = cs.onSecondaryContainer, modifier = Modifier.fillMaxSize().padding(9.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            trailing?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }
    }
}
