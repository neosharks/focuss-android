package com.focuss.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device persistence for schedules. Uses SharedPreferences with a hand-rolled
 * JSON encoding (org.json ships with Android, so no third-party serializer needed).
 * Everything stays local — nothing is ever uploaded.
 */
class ScheduleStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Monotonic counter bumped on every write — lets readers cache cheaply. */
    val revision: Long get() = prefs.getLong(REVISION_KEY, 0L)

    fun load(): List<Schedule> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { decode(raw) }.getOrDefault(emptyList())
    }

    fun saveAll(schedules: List<Schedule>) {
        prefs.edit()
            .putString(KEY, encode(schedules))
            .putLong(REVISION_KEY, revision + 1)
            .apply()
    }

    fun upsert(schedule: Schedule): List<Schedule> {
        val all = load().toMutableList()
        val idx = all.indexOfFirst { it.id == schedule.id }
        if (idx >= 0) all[idx] = schedule else all.add(schedule)
        saveAll(all)
        return all
    }

    fun delete(id: String): List<Schedule> {
        val remaining = load().filterNot { it.id == id }
        saveAll(remaining)
        return remaining
    }

    // ── JSON encoding ───────────────────────────────────────────────────────

    private fun encode(schedules: List<Schedule>): String {
        val arr = JSONArray()
        for (s in schedules) {
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("enabled", s.enabled)
                    put("startHour", s.startTime.hour)
                    put("startMinute", s.startTime.minute)
                    put("endHour", s.endTime.hour)
                    put("endMinute", s.endTime.minute)
                    put("days", JSONArray(s.days.toList()))
                    put("blockedApps", JSONArray(s.blockedApps.toList()))
                    put("disableCooldown", s.disableCooldown)
                    put("createdAt", s.createdAt)
                },
            )
        }
        return arr.toString()
    }

    private fun decode(raw: String): List<Schedule> {
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Schedule(
                id = o.getString("id"),
                name = o.getString("name"),
                enabled = o.optBoolean("enabled", true),
                startTime = TimeSlot(o.getInt("startHour"), o.getInt("startMinute")),
                endTime = TimeSlot(o.getInt("endHour"), o.getInt("endMinute")),
                days = o.getJSONArray("days").toIntSet(),
                blockedApps = o.getJSONArray("blockedApps").toStringSet(),
                disableCooldown = o.optInt("disableCooldown", 10),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }

    private fun JSONArray.toIntSet(): Set<Int> =
        (0 until length()).map { getInt(it) }.toSet()

    private fun JSONArray.toStringSet(): Set<String> =
        (0 until length()).map { getString(it) }.toSet()

    companion object {
        private const val PREFS = "focuss_schedules"
        private const val KEY = "schedules"
        private const val REVISION_KEY = "revision"
    }
}
