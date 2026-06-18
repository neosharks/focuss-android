package com.focuss.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuss.R
import com.focuss.data.Schedule
import com.focuss.data.ScheduleLogic
import com.focuss.data.TimeSlot
import com.focuss.ui.components.SectionLabel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    initial: Schedule,
    others: List<Schedule>,
    onChange: (Schedule) -> Unit,
    onEditApps: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    var draft by remember { mutableStateOf(initial) }
    var showDelete by remember { mutableStateOf(false) }

    // Conflicting schedule (shares a day and time window), if any. Editing is always
    // allowed, but an overlapping draft is never persisted.
    val conflict = remember(draft, others) { ScheduleLogic.firstConflict(draft, others) }

    fun update(transform: (Schedule) -> Schedule) {
        val next = transform(draft)
        draft = next
        val valid = next.name.isNotBlank() && next.days.isNotEmpty() &&
            ScheduleLogic.firstConflict(next, others) == null
        if (valid) onChange(next)
    }

    LaunchedEffect(initial.blockedApps) {
        if (initial.blockedApps != draft.blockedApps) draft = draft.copy(blockedApps = initial.blockedApps)
    }

    fun pickTime(current: TimeSlot, onPicked: (TimeSlot) -> Unit) {
        TimePickerDialog(context, { _, h, m -> onPicked(TimeSlot(h, m)) }, current.hour, current.minute, false).show()
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (initial.name.isNotBlank() && initial.name != "New Schedule") R.string.edit_schedule else R.string.new_schedule,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                },
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
            if (conflict != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cs.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = cs.onErrorContainer, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.overlap_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onErrorContainer,
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.schedule_name), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { text -> update { it.copy(name = text) } },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.primary,
                    unfocusedBorderColor = cs.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeCard(stringResource(R.string.start_time), draft.startTime, true, Modifier.weight(1f)) {
                    pickTime(draft.startTime) { t -> update { it.copy(startTime = t) } }
                }
                TimeCard(stringResource(R.string.end_time), draft.endTime, true, Modifier.weight(1f)) {
                    pickTime(draft.endTime) { t -> update { it.copy(endTime = t) } }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.active_days), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        (0..6).forEach { day ->
                            val selected = day in draft.days
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp).clip(CircleShape)
                                    .then(if (selected) Modifier.background(cs.primary) else Modifier.background(cs.surfaceContainerHighest))
                                    .clickable {
                                        update { it.copy(days = if (selected) it.days - day else it.days + day) }
                                    },
                            ) {
                                Text(
                                    ScheduleLogic.DAY_INITIALS[day],
                                    color = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            stringResource(R.string.every_day) to ScheduleLogic.ALL_DAYS,
                            stringResource(R.string.weekdays) to ScheduleLogic.WEEKDAYS,
                            stringResource(R.string.weekends) to ScheduleLogic.WEEKENDS,
                        ).forEach { (label, days) ->
                            val active = draft.days == days
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) cs.secondaryContainer else cs.surfaceContainerHighest,
                                modifier = Modifier.weight(1f).clickable { update { it.copy(days = days) } },
                            ) {
                                Text(
                                    label,
                                    color = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
                                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.blocked_apps), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    val n = draft.blockedApps.size
                    Text(
                        when {
                            n == 0 -> stringResource(R.string.no_apps_selected)
                            n == 1 -> stringResource(R.string.one_app_blocked)
                            else -> stringResource(R.string.apps_blocked, n)
                        },
                        style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.weight(1f),
                    )
                    run {
                        Surface(
                            shape = RoundedCornerShape(12.dp), color = cs.secondaryContainer,
                            modifier = Modifier.clickable { onEditApps(draft.id) },
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) {
                                Icon(Icons.Rounded.Edit, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.edit), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.onSecondaryContainer)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(stringResource(R.string.disable_wait_time), modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.disable_wait_desc), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30, 60).forEach { secs ->
                            val active = draft.disableCooldown == secs
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (active) cs.primary else cs.surfaceContainerHighest,
                                modifier = Modifier.weight(1f).clickable { update { it.copy(disableCooldown = secs) } },
                            ) {
                                Text(
                                    if (secs >= 60) "${secs / 60}m" else "${secs}s",
                                    color = if (active) cs.onPrimary else cs.onSurface,
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                }
            }

            run {
                Spacer(Modifier.height(28.dp))
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = cs.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_schedule), color = cs.error, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_schedule)) },
            text = { Text(stringResource(R.string.delete_confirm, draft.name)) },
            confirmButton = {
                TextButton(onClick = { showDelete = false; onDelete(draft.id) }) { Text(stringResource(R.string.delete), color = cs.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.keep)) } },
        )
    }
}

@Composable
private fun TimeCard(label: String, time: TimeSlot, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
    val ampm = if (time.hour < 12) "AM" else "PM"
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(label, fontSize = 11.sp, color = cs.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "%d:%02d".format(hour12, time.minute),
                fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = cs.onSurface,
            )
            Text(ampm, fontSize = 12.sp, color = cs.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
