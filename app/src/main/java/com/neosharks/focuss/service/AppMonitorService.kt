package com.neosharks.focuss.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.neosharks.focuss.data.Prefs

/**
 * Foreground service that keeps the process alive and shows the persistent status
 * notification. It deliberately holds no blocking state — detection and the
 * "what's blocked now" decision live entirely in [BlockingAccessibilityService],
 * which reads from storage. This service just guarantees a living process and a
 * user-visible indicator, and restarts itself if the task is swiped away.
 */
class AppMonitorService : Service() {

    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs.protectionEnabled = true
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        isRunning = true
        return START_STICKY
    }

    /** Swiping the app from recents must NOT stop protection — re-arm ourselves. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (prefs.protectionEnabled) {
            val restart = Intent(applicationContext, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restart)
            } else {
                applicationContext.startService(restart)
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focuss is on")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pi)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true) // no sound or vibration
            .setPriority(NotificationCompat.PRIORITY_MIN) // pre-O: minimal, no status-bar icon
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET) // hidden from the lock screen
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Focuss status", NotificationManager.IMPORTANCE_MIN,
            ).apply {
                description = "Minimal, silent indicator that Focuss is running"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                // Don't reveal anything on the lock screen.
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.deleteNotificationChannel("focuss_channel") // remove the old louder channel
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        @Volatile
        var isRunning = false
            private set

        // New id (the old "focuss_channel" kept its importance once created).
        private const val CHANNEL_ID = "focuss_status"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitorService::class.java))
        }
    }
}
