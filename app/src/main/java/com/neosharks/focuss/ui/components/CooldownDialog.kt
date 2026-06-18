package com.neosharks.focuss.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.neosharks.focuss.R
import kotlinx.coroutines.delay

/**
 * A deliberate friction step: when the user disables protection, they must wait
 * out a countdown (configured per-schedule) before it actually turns off. Encourages
 * them to reconsider and stay focused.
 */
@Composable
fun CooldownDialog(
    durationSeconds: Int,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
) {
    var remaining by remember { mutableIntStateOf(durationSeconds) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        onComplete()
    }

    val cs = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = if (durationSeconds > 0) remaining.toFloat() / durationSeconds else 0f,
        label = "cooldown",
    )

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cs.surface,
            tonalElevation = 4.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(cs.primary.copy(alpha = 0.12f))
                        .padding(16.dp),
                )
                Text(
                    stringResource(R.string.hold_on),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(
                    stringResource(R.string.cooldown_message),
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "$remaining",
                    color = cs.primary,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(stringResource(R.string.seconds), color = cs.onSurfaceVariant, fontSize = 13.sp)

                LinearProgressIndicator(
                    progress = { progress },
                    color = if (progress > 0.5f) cs.tertiary else cs.error,
                    trackColor = cs.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(99.dp)),
                )

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                ) {
                    Text(stringResource(R.string.cooldown_cancel), color = cs.onSurfaceVariant)
                }
            }
        }
    }
}
