package com.focuss.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuss.R
import com.focuss.data.ScheduleLogic
import com.focuss.ui.UiState
import com.focuss.ui.components.CooldownDialog
import com.focuss.ui.components.ScheduleCard
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onOpenSettings: () -> Unit,
    onFixService: () -> Unit,
    onStartProtection: () -> Unit,
    onStopProtection: () -> Unit,
    onToggleSchedule: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onOpenSchedule: (String) -> Unit,
    onLockedAttempt: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var showCooldown by remember { mutableStateOf(false) }

    // 1-second ticker for the live timer.
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            tick = System.currentTimeMillis()
            delay(1000)
        }
    }

    val active = state.activeSession
    val locked = state.sessionLocked

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    Icon(
                        Icons.Rounded.GppGood, null, tint = cs.primary,
                        modifier = Modifier.padding(start = 16.dp).size(24.dp),
                    )
                },
                actions = {
                    androidx.compose.material3.FilledTonalIconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background),
            )
        },
        floatingActionButton = {
            if (state.schedules.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddSchedule,
                    containerColor = cs.primaryContainer,
                    contentColor = cs.onPrimaryContainer,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text(stringResource(R.string.new_schedule)) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            // Warn when protection is on but the OS disabled the accessibility service.
            if (state.serviceSilentlyOff) {
                Surface(
                    onClick = onFixService,
                    shape = RoundedCornerShape(18.dp),
                    color = cs.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = cs.onErrorContainer, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.service_disabled_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onErrorContainer,
                        )
                    }
                }
            }

            // Protection hero
            ProtectionCard(
                on = state.protectionOn,
                onToggle = { if (state.protectionOn) showCooldown = true else onStartProtection() },
            )

            // Live session timer — circular ring around the digital countdown.
            AnimatedVisibility(visible = active != null) {
                if (active != null) {
                    val now = remember(tick) { Calendar.getInstance() }
                    val remaining = ScheduleLogic.secondsRemaining(active, now)
                    val progress = ScheduleLogic.progress(active, now)
                    val fill by animateFloatAsState(targetValue = 1f - progress, label = "fill")

                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = cs.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                        ) {
                            Text(
                                active.name.uppercase(),
                                color = cs.onTertiaryContainer.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                ScheduleLogic.formatClock(remaining),
                                color = cs.onTertiaryContainer,
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(stringResource(R.string.remaining), color = cs.onTertiaryContainer.copy(alpha = 0.6f), fontSize = 14.sp)
                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(cs.onTertiaryContainer.copy(alpha = 0.15f)),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fill)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(cs.tertiary),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                stringResource(R.string.your_schedules),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            if (state.schedules.isEmpty()) {
                EmptyState(onAdd = onAddSchedule)
            } else {
                state.schedules.forEach { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        locked = locked,
                        onClick = { if (locked) onLockedAttempt() else onOpenSchedule(schedule.id) },
                        onToggle = { onToggleSchedule(schedule.id) },
                    )
                }
            }

            Spacer(Modifier.height(88.dp)) // clear the FAB
        }
    }

    if (showCooldown) {
        CooldownDialog(
            durationSeconds = active?.disableCooldown ?: 10,
            onCancel = { showCooldown = false },
            onComplete = {
                showCooldown = false
                onStopProtection()
            },
        )
    }
}

@Composable
private fun ProtectionCard(on: Boolean, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (on) cs.primaryContainer else cs.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (on) cs.primary.copy(alpha = 0.25f) else cs.surfaceContainerHighest,
                    ),
            ) {
                Icon(
                    if (on) Icons.Rounded.GppGood else Icons.Rounded.GppMaybe,
                    contentDescription = null,
                    tint = if (on) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.protection),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) cs.onPrimaryContainer.copy(alpha = 0.7f) else cs.onSurfaceVariant,
                )
                Text(
                    stringResource(if (on) R.string.active else R.string.paused),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (on) cs.onPrimaryContainer else cs.onSurface,
                )
            }
            Switch(checked = on, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp))
                .background(cs.secondaryContainer),
        ) {
            Icon(
                Icons.Outlined.CalendarMonth, null,
                tint = cs.onSecondaryContainer, modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.no_schedules_title), style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.no_schedules_desc),
            color = cs.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        ExtendedFloatingActionButton(
            onClick = onAdd,
            containerColor = cs.primaryContainer,
            contentColor = cs.onPrimaryContainer,
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text(stringResource(R.string.add_first_schedule)) },
        )
    }
}
