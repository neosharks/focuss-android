package com.focuss.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focuss.FocussApp
import com.focuss.data.InstalledApp
import com.focuss.data.Schedule
import com.focuss.data.ScheduleLogic
import com.focuss.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val schedules: List<Schedule> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = false,
    val hasUsage: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasAccessibility: Boolean = false,
    val protectionOn: Boolean = false,
    val batteryExempt: Boolean = false,
    val strictMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: String = "",
) {
    val allPermissionsGranted: Boolean get() = hasUsage && hasOverlay && hasAccessibility
    val grantedCount: Int get() = listOf(hasUsage, hasOverlay, hasAccessibility).count { it }

    /** The schedule currently enforcing, if protection is on. */
    val activeSession: Schedule?
        get() = if (protectionOn) ScheduleLogic.activeSession(schedules) else null

    val sessionLocked: Boolean get() = activeSession != null

    /** Protection is on but the OS turned the accessibility service off → blocking is silently dead. */
    val serviceSilentlyOff: Boolean get() = protectionOn && !hasAccessibility
}

class FocussViewModel(app: Application) : AndroidViewModel(app) {

    private val store = (app as FocussApp).scheduleStore
    private val repo = (app as FocussApp).appRepository
    private val prefs = (app as FocussApp).prefs

    private val _state = MutableStateFlow(
        UiState(
            schedules = store.load(),
            themeMode = prefs.themeMode,
            appLanguage = prefs.appLanguage,
            protectionOn = repo.isProtectionOn,
            batteryExempt = repo.isIgnoringBatteryOptimizations(),
            strictMode = repo.isStrictModeActive(),
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        // If protection was left on (e.g. after a reboot or process death), make
        // sure the keep-alive service is actually running.
        repo.ensureServiceMatchesState()
        refresh()
    }

    /** Re-read permissions + protection + schedules from the system. */
    fun refresh() {
        val schedules = store.load()
        _state.update {
            it.copy(
                schedules = schedules,
                hasUsage = repo.hasUsageStatsPermission(),
                hasOverlay = repo.hasOverlayPermission(),
                hasAccessibility = repo.hasAccessibilityPermission(),
                protectionOn = repo.isProtectionOn,
                batteryExempt = repo.isIgnoringBatteryOptimizations(),
                strictMode = repo.isStrictModeActive(),
            )
        }
    }

    // ── Schedules ─────────────────────────────────────────────────────────────

    fun upsertSchedule(schedule: Schedule) {
        val updated = store.upsert(schedule)
        _state.update { it.copy(schedules = updated) }
        // No need to push anything — the accessibility service reads storage live.
    }

    fun deleteSchedule(id: String) {
        val updated = store.delete(id)
        _state.update { it.copy(schedules = updated) }
    }

    fun toggleSchedule(id: String) {
        val target = state.value.schedules.firstOrNull { it.id == id } ?: return
        upsertSchedule(target.copy(enabled = !target.enabled))
    }

    fun scheduleById(id: String?): Schedule? =
        state.value.schedules.firstOrNull { it.id == id }

    // ── Installed apps ─────────────────────────────────────────────────────────

    fun loadInstalledApps() {
        if (state.value.installedApps.isNotEmpty() || state.value.appsLoading) return
        _state.update { it.copy(appsLoading = true) }
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { repo.loadInstalledApps() }
            _state.update { it.copy(installedApps = apps, appsLoading = false) }
        }
    }

    /** Lazily decode a single app's icon off the main thread (cached in the repo). */
    suspend fun loadIcon(packageName: String): Bitmap? =
        withContext(Dispatchers.IO) { repo.loadIcon(packageName) }

    // ── Protection control ───────────────────────────────────────────────────

    fun startProtection() {
        repo.startBlocking()
        _state.update { it.copy(protectionOn = true) }
    }

    fun stopProtection() {
        repo.stopBlocking()
        _state.update { it.copy(protectionOn = false) }
    }

    // ── Permissions ─────────────────────────────────────────────────────────────

    fun requestUsage() = repo.openUsageAccessSettings()
    fun requestOverlay() = repo.openOverlaySettings()
    fun requestAccessibility() = repo.openAccessibilitySettings()

    // ── Reliability / strict mode ──────────────────────────────────────────────

    fun requestBatteryExemption() = repo.requestIgnoreBatteryOptimizations()

    /** Caller launches the returned intent (admin enable must come from an Activity). */
    fun strictModeEnableIntent() = repo.strictModeEnableIntent()

    fun disableStrictMode() {
        repo.disableStrictMode()
        _state.update { it.copy(strictMode = false) }
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    /** Persist the chosen language. The activity recreates to apply it. */
    fun setLanguage(tag: String) {
        prefs.appLanguage = tag
        _state.update { it.copy(appLanguage = tag) }
    }

    /** Cycle System → Light → Dark → System. Starts from the device theme. */
    fun cycleTheme() {
        val next = when (state.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        prefs.themeMode = next
        _state.update { it.copy(themeMode = next) }
    }
}
