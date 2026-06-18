package com.neosharks.focuss.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neosharks.focuss.data.Schedule
import com.neosharks.focuss.data.ScheduleLogic

@Composable
fun ScheduleCard(
    schedule: Schedule,
    locked: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val active = ScheduleLogic.isActiveNow(schedule)

    Card(
        onClick = onClick, // always clickable so a locked tap can show feedback
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) cs.secondaryContainer else cs.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Title + switch
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(cs.tertiary),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    schedule.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) cs.onSecondaryContainer else cs.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (locked) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    )
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = { if (!locked) onToggle() },
                    enabled = !locked,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Time row + active pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "${ScheduleLogic.formatTime(schedule.startTime)} – ${ScheduleLogic.formatTime(schedule.endTime)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(cs.tertiary)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "ACTIVE",
                            color = cs.onTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Day chips + app count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0..6).forEach { day ->
                        val on = day in schedule.days
                        val chipBg = when {
                            on && active -> cs.tertiary.copy(alpha = 0.30f)
                            on -> cs.primary.copy(alpha = 0.16f)
                            else -> cs.surfaceContainerHighest.copy(alpha = 0.6f)
                        }
                        val chipFg = when {
                            on && active -> cs.onSecondaryContainer
                            on -> cs.primary
                            else -> cs.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(chipBg),
                        ) {
                            Text(
                                ScheduleLogic.DAY_INITIALS[day],
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipFg,
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Apps,
                        contentDescription = null,
                        tint = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    val n = schedule.blockedApps.size
                    Text(
                        "$n",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) cs.onSecondaryContainer else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
