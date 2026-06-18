package com.focuss.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focuss.data.Prefs

/**
 * After a reboot the system re-enables the accessibility service on its own, but
 * the foreground process and its notification are gone. If the user had
 * protection on, bring the monitor service back so the indicator is restored and
 * the process stays resident.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            if (Prefs(context).protectionEnabled) {
                AppMonitorService.start(context)
            }
        }
    }
}
