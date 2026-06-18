package com.neosharks.focuss.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neosharks.focuss.R
import com.neosharks.focuss.ui.UiState

private data class PermItem(
    val title: String,
    val description: String,
    val granted: Boolean,
    val icon: ImageVector,
    val onGrant: () -> Unit,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    state: UiState,
    onRequestUsage: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestBattery: () -> Unit,
    onContinue: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val perms = listOf(
        PermItem(stringResource(R.string.perm_usage_title), stringResource(R.string.perm_usage_desc), state.hasUsage, Icons.Outlined.BarChart, onRequestUsage),
        PermItem(stringResource(R.string.perm_overlay_title), stringResource(R.string.perm_overlay_desc), state.hasOverlay, Icons.Outlined.Layers, onRequestOverlay),
        PermItem(stringResource(R.string.perm_accessibility_title), stringResource(R.string.perm_accessibility_desc), state.hasAccessibility, Icons.Outlined.Accessibility, onRequestAccessibility),
    )
    val grantedCount = state.grantedCount
    val allDone = grantedCount == 3

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = { Text("Focuss", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(Icons.Rounded.GppGood, null, tint = cs.primary, modifier = Modifier.padding(start = 16.dp).size(24.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
            )
        },
        bottomBar = {
            Button(
                onClick = onContinue,
                enabled = allDone,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    disabledContainerColor = cs.surfaceContainerHighest,
                ),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            ) {
                Text(
                    if (allDone) stringResource(R.string.continue_to_focuss) else stringResource(R.string.more_needed, 3 - grantedCount),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(96.dp).clip(CircleShape).background(cs.primaryContainer),
            ) {
                Icon(Icons.Rounded.GppGood, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.setup_required), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_subtitle),
                color = cs.onSurfaceVariant, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    val filled = i < grantedCount
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (filled) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(
                                when {
                                    filled && allDone -> cs.tertiary
                                    filled -> cs.primary
                                    else -> cs.surfaceContainerHighest
                                },
                            ),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (allDone) stringResource(R.string.all_set) else stringResource(R.string.granted_progress, grantedCount),
                fontSize = 12.sp, color = cs.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            perms.forEachIndexed { index, p -> PermCard(index + 1, p) }

            // Recommended (non-blocking): battery exemption for reliability.
            if (!state.batteryExempt) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.reliability_title), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.reliability_desc), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        FilledTonalButton(onClick = onRequestBattery, shape = RoundedCornerShape(99.dp)) {
                            Text(stringResource(R.string.allow), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(cs.surfaceContainer).padding(16.dp),
            ) {
                Icon(Icons.Rounded.Lock, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.privacy_note),
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermCard(number: Int, p: PermItem) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (p.granted) cs.secondaryContainer else cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Row(modifier = Modifier.padding(18.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(if (p.granted) cs.tertiary else cs.surfaceContainerHighest),
            ) {
                if (p.granted) {
                    Icon(Icons.Rounded.Check, null, tint = cs.onTertiary, modifier = Modifier.size(18.dp))
                } else {
                    Text("$number", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = cs.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    p.title, style = MaterialTheme.typography.titleMedium,
                    color = if (p.granted) cs.onSecondaryContainer else cs.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    p.description, style = MaterialTheme.typography.bodyMedium,
                    color = if (p.granted) cs.onSecondaryContainer.copy(alpha = 0.8f) else cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                if (p.granted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = cs.tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.granted), color = cs.onSecondaryContainer, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    FilledTonalButton(
                        onClick = p.onGrant,
                        shape = RoundedCornerShape(99.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary,
                        ),
                    ) {
                        Icon(p.icon, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.grant_access), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
