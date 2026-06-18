package com.focuss.data

/** A point in the day, 24-hour based. */
data class TimeSlot(val hour: Int, val minute: Int) {
    /** Minutes since midnight — handy for comparisons. */
    val asMinutes: Int get() = hour * 60 + minute
}

/**
 * A single focus rule: which apps to block, when, and how hard it is to switch off.
 * Days use 0 = Sunday … 6 = Saturday (matches Calendar.DAY_OF_WEEK - 1).
 */
data class Schedule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val startTime: TimeSlot,
    val endTime: TimeSlot,
    val days: Set<Int>,
    val blockedApps: Set<String>,
    val disableCooldown: Int, // seconds the user must wait before protection turns off
    val createdAt: Long,
)

/**
 * A launchable app the user can choose to block. Icons are intentionally *not*
 * carried here — they're loaded lazily per visible row (see AppRepository.loadIcon)
 * so the picker can show the list instantly instead of decoding ~200 icons up front.
 */
data class InstalledApp(
    val packageName: String,
    val appName: String,
)
