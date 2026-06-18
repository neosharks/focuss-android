package com.neosharks.focuss.data

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.graphics.createBitmap
import com.neosharks.focuss.R
import com.neosharks.focuss.service.AppMonitorService
import com.neosharks.focuss.service.BlockingAccessibilityService
import com.neosharks.focuss.service.UninstallProtectionAdmin

/**
 * The bridge between the UI and the Android system: enumerating launchable apps,
 * checking/requesting the three required special permissions, and starting or
 * stopping the blocking service.
 */
class AppRepository(private val context: Context) {

    private val appContext = context.applicationContext

    // ── Installed apps ────────────────────────────────────────────────────────

    /** Decoded icons, keyed by package name. Bounded by the number of installed apps. */
    private val iconCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()

    /**
     * All launchable apps except Focuss itself, sorted by name. Cheap — just names
     * and package ids, no icon decoding. Icons are fetched lazily via [loadIcon].
     */
    fun loadInstalledApps(): List<InstalledApp> {
        val pm = appContext.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.packageName != appContext.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    appName = pm.getApplicationLabel(info).toString(),
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    /** Decodes (and caches) a single app's icon. Call off the main thread. */
    fun loadIcon(packageName: String): Bitmap? {
        iconCache[packageName]?.let { return it }
        return runCatching {
            appContext.packageManager.getApplicationIcon(packageName).toScaledBitmap(96)
        }.getOrNull()?.also { iconCache[packageName] = it }
    }

    private fun Drawable.toScaledBitmap(size: Int): Bitmap {
        val source = if (this is BitmapDrawable && bitmap != null) {
            bitmap
        } else {
            val w = intrinsicWidth.coerceAtLeast(1)
            val h = intrinsicHeight.coerceAtLeast(1)
            createBitmap(w, h).also { bmp ->
                val canvas = Canvas(bmp)
                setBounds(0, 0, w, h)
                draw(canvas)
            }
        }
        return Bitmap.createScaledBitmap(source, size, size, true)
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    fun hasUsageStatsPermission(): Boolean {
        val appOps = appContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), appContext.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(appContext)

    fun hasAccessibilityPermission(): Boolean {
        val enabled = Settings.Secure.getString(
            appContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: ""
        val component = "${appContext.packageName}/${BlockingAccessibilityService::class.java.name}"
        return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }

    fun allPermissionsGranted(): Boolean =
        hasUsageStatsPermission() && hasOverlayPermission() && hasAccessibilityPermission()

    fun openUsageAccessSettings() = startSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openOverlaySettings() = startSettings(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${appContext.packageName}"),
        ),
    )

    fun openAccessibilitySettings() = startSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    private fun startSettings(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    // ── Protection control ───────────────────────────────────────────────────
    //
    // "Protection" is a persisted master switch (see Prefs). The accessibility
    // service decides moment-to-moment what to block by reading the schedules
    // itself, so turning protection on/off here is just flipping that flag and
    // running (or stopping) the keep-alive foreground service.

    private val prefs = Prefs(appContext)

    /** True when protection is enabled — survives process death, unlike a live service flag. */
    val isProtectionOn: Boolean get() = prefs.protectionEnabled

    fun startBlocking() {
        prefs.protectionEnabled = true
        AppMonitorService.start(appContext)
    }

    fun stopBlocking() {
        prefs.protectionEnabled = false
        AppMonitorService.stop(appContext)
    }

    /** If protection should be on but the keep-alive service isn't, re-arm it. */
    fun ensureServiceMatchesState() {
        if (prefs.protectionEnabled && !AppMonitorService.isRunning) {
            AppMonitorService.start(appContext)
        }
    }

    // ── Battery optimisation ───────────────────────────────────────────────────

    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(appContext.packageName)
    }

    /** Opens the system prompt asking to exempt Focuss from battery optimisation. */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations() {
        if (isIgnoringBatteryOptimizations()) {
            startSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            return
        }
        startSettings(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${appContext.packageName}"),
            ),
        )
    }

    // ── Strict mode (device admin → uninstall protection) ──────────────────────

    private val dpm: DevicePolicyManager
        get() = appContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent: ComponentName
        get() = ComponentName(appContext, UninstallProtectionAdmin::class.java)

    fun isStrictModeActive(): Boolean = dpm.isAdminActive(adminComponent)

    /** Returns the intent the UI should launch to ask the user to enable the admin. */
    fun strictModeEnableIntent(): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                appContext.getString(R.string.device_admin_description),
            )
        }

    fun disableStrictMode() {
        if (isStrictModeActive()) dpm.removeActiveAdmin(adminComponent)
        prefs.strictMode = false
    }
}
