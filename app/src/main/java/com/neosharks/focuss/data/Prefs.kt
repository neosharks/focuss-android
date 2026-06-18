package com.neosharks.focuss.data

import android.content.Context

/** How the app decides light vs. dark. Defaults to following the device. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Small shared key/value store. Read from several processes: the UI, the
 * out-of-process [com.neosharks.focuss.block.BlockingActivity], and the always-on
 * [com.neosharks.focuss.service.BlockingAccessibilityService]. Everything here must be
 * cheap to read and safe to read from any of them.
 */
class Prefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    init {
        // One-time cleanup: drop the legacy boolean theme key from older builds.
        if (prefs.contains("dark_theme")) prefs.edit().remove("dark_theme").apply()
    }

    // ── Theme ────────────────────────────────────────────────────────────────

    var themeMode: ThemeMode
        get() = when (prefs.getString("theme_mode", null)) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        set(value) = prefs.edit().putString("theme_mode", value.name.lowercase()).apply()

    /** BCP-47 language tag, or "" to follow the system language. */
    var appLanguage: String
        get() = prefs.getString("app_language", "") ?: ""
        set(value) = prefs.edit().putString("app_language", value).apply()

    // ── Protection master switch (persisted so it survives process death) ──────

    var protectionEnabled: Boolean
        get() = prefs.getBoolean("protection_enabled", false)
        set(value) = prefs.edit().putBoolean("protection_enabled", value).apply()

    // ── Strict mode (uninstall protection via device admin) ────────────────────

    var strictMode: Boolean
        get() = prefs.getBoolean("strict_mode", false)
        set(value) = prefs.edit().putBoolean("strict_mode", value).apply()

    // ── Schedule cache invalidation ────────────────────────────────────────────
    // Bumped whenever schedules change so the accessibility service knows to
    // reload from disk instead of reading on every single window event.

    var scheduleRevision: Long
        get() = prefs.getLong("schedule_revision", 0L)
        set(value) = prefs.edit().putLong("schedule_revision", value).apply()

    // ── Currently running session (shown by the block screen) ──────────────────

    fun saveSession(name: String, start: TimeSlot, end: TimeSlot) {
        prefs.edit()
            .putString("session_name", name)
            .putInt("session_start_hour", start.hour)
            .putInt("session_start_minute", start.minute)
            .putInt("session_end_hour", end.hour)
            .putInt("session_end_minute", end.minute)
            .apply()
    }

    val sessionName: String get() = prefs.getString("session_name", "Focus Session") ?: "Focus Session"
    val sessionStart: TimeSlot
        get() = TimeSlot(prefs.getInt("session_start_hour", 0), prefs.getInt("session_start_minute", 0))
    val sessionEnd: TimeSlot
        get() = TimeSlot(prefs.getInt("session_end_hour", 0), prefs.getInt("session_end_minute", 0))

    companion object {
        private const val NAME = "focuss_prefs"
    }
}
