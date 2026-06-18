package com.focuss.block

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focuss.R
import com.focuss.data.Prefs
import com.focuss.data.ScheduleLogic
import com.focuss.ui.theme.FocussTheme
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Full-screen "this app is blocked" wall. Launched by the accessibility service
 * over any restricted app. Shows the live countdown to the end of the session
 * and offers only graceful exits (Home / open Focuss).
 */
class BlockingActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.focuss.data.LocaleHelper.wrap(newBase))
    }

    override fun onResume() { super.onResume(); isShowing = true }
    override fun onPause() { super.onPause(); isShowing = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val prefs = Prefs(this)
        val blockedPkg = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()
        val appName = resolveLabel(blockedPkg)

        val now = Calendar.getInstance()
        val totalSeconds = run {
            val start = prefs.sessionStart.hour * 3600L + prefs.sessionStart.minute * 60L
            val end = prefs.sessionEnd.hour * 3600L + prefs.sessionEnd.minute * 60L
            var total = end - start
            if (total <= 0) total += 24 * 3600
            total
        }
        val remainingSeconds = run {
            val end = prefs.sessionEnd.hour * 3600L + prefs.sessionEnd.minute * 60L
            val cur = now.get(Calendar.HOUR_OF_DAY) * 3600L +
                now.get(Calendar.MINUTE) * 60L + now.get(Calendar.SECOND)
            var rem = end - cur
            if (rem <= 0) rem += 24 * 3600
            rem
        }

        setContent {
            FocussTheme(themeMode = prefs.themeMode) {
                BlockWall(
                    appName = appName,
                    sessionName = prefs.sessionName,
                    initialRemaining = remainingSeconds,
                    totalSeconds = totalSeconds,
                    onHome = ::goHome,
                    onOpenFocuss = ::openFocuss,
                )
            }
        }
    }

    private fun resolveLabel(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
        finish()
    }

    private fun openFocuss() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
        finish()
    }

    @Suppress("MissingSuperCall", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() = goHome()

    companion object {
        const val EXTRA_PACKAGE = "blockedPackage"

        @Volatile
        var isShowing = false
            private set
    }
}

@Composable
private fun BlockWall(
    appName: String,
    sessionName: String,
    initialRemaining: Long,
    totalSeconds: Long,
    onHome: () -> Unit,
    onOpenFocuss: () -> Unit,
) {
    val startElapsed = remember { SystemClock.elapsedRealtime() }
    var remaining by remember { mutableLongStateOf(initialRemaining) }

    LaunchedEffect(Unit) {
        while (remaining > 0) {
            delay(1000)
            val elapsed = (SystemClock.elapsedRealtime() - startElapsed) / 1000
            remaining = (initialRemaining - elapsed).coerceAtLeast(0)
        }
    }

    val target = if (totalSeconds > 0) (remaining.toFloat() / totalSeconds.toFloat()) else 0f
    val progress by animateFloatAsState(
        targetValue = target.coerceIn(0f, 1f),
        animationSpec = tween(800, easing = LinearEasing),
        label = "progress",
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(28.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 2.dp,
                shadowElevation = 12.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    ) {
                        Text("🛡️", fontSize = 32.sp)
                    }

                    Text(
                        stringResource(R.string.focus_mode).uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        stringResource(R.string.app_blocked),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        stringResource(R.string.app_paused, appName),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
                    )

                    Text(
                        sessionName.uppercase(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 28.dp),
                    )
                    Text(
                        ScheduleLogic.formatClock(remaining),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        stringResource(R.string.remaining),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(99.dp)),
                    )

                    Button(
                        onClick = onHome,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                    ) {
                        Text(stringResource(R.string.go_back_home), fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onOpenFocuss, modifier = Modifier.padding(top = 4.dp)) {
                        Text(stringResource(R.string.open_focuss))
                    }
                }
            }
        }
    }
}
