package com.neosharks.focuss.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the pure scheduling logic — especially the fiddly bits:
 * overnight windows, boundary conditions, and the live-session math.
 */
class ScheduleLogicTest {

    /** Build a Calendar fixed to a given weekday + time. day: 0=Sun … 6=Sat. */
    private fun cal(day: Int, hour: Int, minute: Int, second: Int = 0): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, day + 1) // Calendar.SUNDAY == 1
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }

    private fun schedule(
        enabled: Boolean = true,
        start: TimeSlot = TimeSlot(9, 0),
        end: TimeSlot = TimeSlot(17, 0),
        days: Set<Int> = ScheduleLogic.WEEKDAYS,
        apps: Set<String> = setOf("com.x"),
    ) = Schedule(
        id = "t", name = "T", enabled = enabled,
        startTime = start, endTime = end, days = days,
        blockedApps = apps, disableCooldown = 10, createdAt = 0,
    )

    @Test fun `active inside a normal daytime window`() {
        // Wednesday 10:00, window 9–17 on weekdays
        assertTrue(ScheduleLogic.isActiveNow(schedule(), cal(3, 10, 0)))
    }

    @Test fun `inactive before start and at or after end`() {
        assertFalse(ScheduleLogic.isActiveNow(schedule(), cal(3, 8, 59)))
        // End is exclusive.
        assertFalse(ScheduleLogic.isActiveNow(schedule(), cal(3, 17, 0)))
        assertTrue(ScheduleLogic.isActiveNow(schedule(), cal(3, 16, 59)))
    }

    @Test fun `inactive on a day not in the set`() {
        // Sunday is not a weekday.
        assertFalse(ScheduleLogic.isActiveNow(schedule(), cal(0, 10, 0)))
    }

    @Test fun `disabled schedule is never active`() {
        assertFalse(ScheduleLogic.isActiveNow(schedule(enabled = false), cal(3, 10, 0)))
    }

    @Test fun `overnight window spanning midnight`() {
        val s = schedule(start = TimeSlot(22, 0), end = TimeSlot(6, 0), days = ScheduleLogic.ALL_DAYS)
        assertTrue(ScheduleLogic.isActiveNow(s, cal(3, 23, 30)))  // late evening
        assertTrue(ScheduleLogic.isActiveNow(s, cal(3, 2, 0)))    // small hours
        assertFalse(ScheduleLogic.isActiveNow(s, cal(3, 12, 0)))  // midday
    }

    @Test fun `activeBlockedApps unions only the schedules active now`() {
        val morning = schedule(start = TimeSlot(8, 0), end = TimeSlot(10, 0), apps = setOf("a"))
        val evening = schedule(start = TimeSlot(18, 0), end = TimeSlot(20, 0), apps = setOf("b"))
        val now = cal(3, 9, 0)
        // Only `morning` is active → only "a".
        val active = listOf(morning, evening).filter { ScheduleLogic.isActiveNow(it, now) }
            .flatMap { it.blockedApps }.toSet()
        assertEquals(setOf("a"), active)
    }

    @Test fun `secondsRemaining counts down to end`() {
        // 16:00 with end 17:00 → 3600s
        assertEquals(3600L, ScheduleLogic.secondsRemaining(schedule(), cal(3, 16, 0, 0)))
    }

    @Test fun `progress is roughly half at the midpoint`() {
        // Window 9–17 (8h), at 13:00 → 50%
        val p = ScheduleLogic.progress(schedule(), cal(3, 13, 0))
        assertTrue("expected ~0.5 but was $p", p in 0.49f..0.51f)
    }

    @Test fun `formatTime renders 12-hour clock`() {
        assertEquals("9:05 AM", ScheduleLogic.formatTime(TimeSlot(9, 5)))
        assertEquals("12:00 PM", ScheduleLogic.formatTime(TimeSlot(12, 0)))
        assertEquals("12:30 AM", ScheduleLogic.formatTime(TimeSlot(0, 30)))
        assertEquals("11:59 PM", ScheduleLogic.formatTime(TimeSlot(23, 59)))
    }

    @Test fun `formatDays recognises presets`() {
        assertEquals("Every day", ScheduleLogic.formatDays(ScheduleLogic.ALL_DAYS))
        assertEquals("Weekdays", ScheduleLogic.formatDays(ScheduleLogic.WEEKDAYS))
        assertEquals("Weekends", ScheduleLogic.formatDays(ScheduleLogic.WEEKENDS))
    }

    @Test fun `overlap when days and times intersect`() {
        val a = schedule(start = TimeSlot(9, 0), end = TimeSlot(11, 0), days = setOf(1, 2, 3))
        val b = schedule(start = TimeSlot(10, 0), end = TimeSlot(12, 0), days = setOf(3, 4))
        assertTrue(ScheduleLogic.overlaps(a.copy(id = "a"), b.copy(id = "b")))
    }

    @Test fun `no overlap when days differ`() {
        val a = schedule(start = TimeSlot(9, 0), end = TimeSlot(11, 0), days = setOf(1))
        val b = schedule(start = TimeSlot(9, 0), end = TimeSlot(11, 0), days = setOf(2))
        assertFalse(ScheduleLogic.overlaps(a.copy(id = "a"), b.copy(id = "b")))
    }

    @Test fun `no overlap when times are adjacent`() {
        val a = schedule(start = TimeSlot(9, 0), end = TimeSlot(11, 0), days = setOf(1))
        val b = schedule(start = TimeSlot(11, 0), end = TimeSlot(13, 0), days = setOf(1))
        assertFalse(ScheduleLogic.overlaps(a.copy(id = "a"), b.copy(id = "b")))
    }

    @Test fun `a schedule never overlaps itself`() {
        val a = schedule()
        assertFalse(ScheduleLogic.overlaps(a, a))
    }

    @Test fun `nonConflictingDefault avoids an existing slot`() {
        val existing = listOf(
            ScheduleLogic.defaultSchedule().copy(
                id = "x", startTime = TimeSlot(9, 0), endTime = TimeSlot(10, 0), days = ScheduleLogic.WEEKDAYS,
            ),
        )
        val fresh = ScheduleLogic.nonConflictingDefault(existing)
        assertEquals(null, ScheduleLogic.firstConflict(fresh, existing))
    }

    @Test fun `formatClock shows hours only when needed`() {
        assertEquals("05:09", ScheduleLogic.formatClock(309))
        assertEquals("1:00:00", ScheduleLogic.formatClock(3600))
    }
}
