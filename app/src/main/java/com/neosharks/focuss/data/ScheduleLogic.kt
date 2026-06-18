package com.neosharks.focuss.data

import java.util.Calendar
import java.util.UUID

/**
 * Pure scheduling helpers — no Android dependencies, easy to reason about and test.
 */
object ScheduleLogic {

    val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val DAY_INITIALS = listOf("S", "M", "T", "W", "T", "F", "S")

    val ALL_DAYS = setOf(0, 1, 2, 3, 4, 5, 6)
    val WEEKDAYS = setOf(1, 2, 3, 4, 5)
    val WEEKENDS = setOf(0, 6)

    fun newId(): String = UUID.randomUUID().toString()

    fun defaultSchedule(): Schedule = Schedule(
        id = newId(),
        name = "New Schedule",
        enabled = true,
        startTime = TimeSlot(9, 0),
        endTime = TimeSlot(17, 0),
        days = WEEKDAYS,
        blockedApps = emptySet(),
        disableCooldown = 10,
        createdAt = System.currentTimeMillis(),
    )

    /** A default schedule whose 1-hour weekday window doesn't clash with [existing]. */
    fun nonConflictingDefault(existing: List<Schedule>): Schedule {
        val base = defaultSchedule()
        val hours = (9..22) + (6..8)
        for (h in hours) {
            val cand = base.copy(startTime = TimeSlot(h, 0), endTime = TimeSlot(h + 1, 0))
            if (firstConflict(cand, existing) == null) return cand
        }
        return base
    }

    fun formatTime(slot: TimeSlot): String {
        val h = slot.hour % 12
        val hour12 = if (h == 0) 12 else h
        val ampm = if (slot.hour < 12) "AM" else "PM"
        return "%d:%02d %s".format(hour12, slot.minute, ampm)
    }

    fun formatDays(days: Set<Int>): String = when {
        days.size == 7 -> "Every day"
        days == WEEKDAYS -> "Weekdays"
        days == WEEKENDS -> "Weekends"
        days.isEmpty() -> "No days"
        else -> days.sorted().joinToString(", ") { DAY_LABELS[it] }
    }

    /** Is this schedule supposed to be enforcing right now? Handles overnight windows. */
    fun isActiveNow(schedule: Schedule, now: Calendar = Calendar.getInstance()): Boolean {
        if (!schedule.enabled) return false
        val today = now.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun … 6=Sat
        if (today !in schedule.days) return false

        val nowMins = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = schedule.startTime.asMinutes
        val end = schedule.endTime.asMinutes

        return if (start <= end) {
            nowMins in start until end
        } else {
            // Overnight: e.g. 22:00 → 06:00
            nowMins >= start || nowMins < end
        }
    }

    /** Union of every package blocked by a schedule that is active right now. */
    fun activeBlockedApps(schedules: List<Schedule>): Set<String> =
        schedules.filter { isActiveNow(it) }
            .flatMap { it.blockedApps }
            .toSet()

    /** The schedule currently running, if any (used for the live session timer). */
    fun activeSession(schedules: List<Schedule>): Schedule? =
        schedules.firstOrNull { isActiveNow(it) }

    /** Seconds left until [schedule] ends, wrapping past midnight if needed. */
    fun secondsRemaining(schedule: Schedule, now: Calendar = Calendar.getInstance()): Long {
        val endSecs = schedule.endTime.hour * 3600L + schedule.endTime.minute * 60L
        val nowSecs = now.get(Calendar.HOUR_OF_DAY) * 3600L +
            now.get(Calendar.MINUTE) * 60L + now.get(Calendar.SECOND)
        var rem = endSecs - nowSecs
        if (rem <= 0) rem += 24 * 3600
        return rem
    }

    /** 0f → 1f progress through the active window. */
    fun progress(schedule: Schedule, now: Calendar = Calendar.getInstance()): Float {
        val startSecs = schedule.startTime.hour * 3600L + schedule.startTime.minute * 60L
        val endSecs = schedule.endTime.hour * 3600L + schedule.endTime.minute * 60L
        val nowSecs = now.get(Calendar.HOUR_OF_DAY) * 3600L +
            now.get(Calendar.MINUTE) * 60L + now.get(Calendar.SECOND)
        var total = endSecs - startSecs; if (total <= 0) total += 24 * 3600
        var elapsed = nowSecs - startSecs; if (elapsed < 0) elapsed += 24 * 3600
        return (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    // ── Overlap detection ──────────────────────────────────────────────────────

    /** A time window split into same-day [lo, hi) minute intervals (overnight → two). */
    private fun intervals(start: TimeSlot, end: TimeSlot): List<IntRange> {
        val s = start.asMinutes
        val e = end.asMinutes
        return if (s < e) listOf(s until e)
        else listOf(s until 1440, 0 until e) // overnight or full-day wrap
    }

    private fun timesOverlap(a: Schedule, b: Schedule): Boolean {
        val ai = intervals(a.startTime, a.endTime)
        val bi = intervals(b.startTime, b.endTime)
        return ai.any { x -> bi.any { y -> x.first < y.last + 1 && y.first < x.last + 1 } }
    }

    /** Two schedules conflict if they share any day and their time windows intersect. */
    fun overlaps(a: Schedule, b: Schedule): Boolean {
        if (a.id == b.id) return false
        if ((a.days intersect b.days).isEmpty()) return false
        return timesOverlap(a, b)
    }

    /** The first other schedule that [candidate] would overlap, or null if it's clear. */
    fun firstConflict(candidate: Schedule, all: List<Schedule>): Schedule? =
        all.firstOrNull { overlaps(candidate, it) }

    fun formatClock(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
