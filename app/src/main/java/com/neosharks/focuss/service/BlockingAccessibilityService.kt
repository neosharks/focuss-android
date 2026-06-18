package com.neosharks.focuss.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.neosharks.focuss.block.BlockingActivity
import com.neosharks.focuss.data.Prefs
import com.neosharks.focuss.data.Schedule
import com.neosharks.focuss.data.ScheduleLogic
import com.neosharks.focuss.data.ScheduleStore

/**
 * The always-on heart of the blocker.
 *
 * Android keeps an enabled accessibility service alive **independently of the
 * app's task** — so this keeps working after the app is swiped from recents, and
 * is auto-started by the system after a reboot. To stay correct in that
 * standalone state it does NOT rely on the UI pushing it a list of packages.
 * Instead, on every foreground change it reads the schedules straight from
 * storage and recomputes what should be blocked *at the current moment*. That is
 * also what makes it automatically pick up the next schedule's time slot once the
 * current one ends — there is no cached window to go stale.
 */
class BlockingAccessibilityService : AccessibilityService() {

    private lateinit var store: ScheduleStore
    private lateinit var prefs: Prefs

    // Cheap in-memory cache of the schedules, refreshed only when the store's
    // revision changes — so we don't parse JSON on every single window event.
    private var cached: List<Schedule> = emptyList()
    private var cachedRevision: Long = -1

    override fun onCreate() {
        super.onCreate()
        store = ScheduleStore(this)
        prefs = Prefs(this)
    }

    private fun schedules(): List<Schedule> {
        val rev = store.revision
        if (rev != cachedRevision) {
            cached = store.load()
            cachedRevision = rev
        }
        return cached
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
                } else {
                    0
                }
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0 // deliver every event with no debounce → react instantly
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return

        // Don't re-trigger while our own wall is up.
        if (BlockingActivity.isShowing) return
        // Master switch off → do nothing.
        if (!prefs.protectionEnabled) return
        // Ignore system chrome and ourselves.
        if (pkg == "com.android.systemui" || pkg == packageName) return

        // Reflect the live schedule (cached; reloads only when it actually changed).
        // Several schedules can overlap, so consider every one active right now.
        val activeNow = schedules().filter { ScheduleLogic.isActiveNow(it) }
        val blocking = activeNow.firstOrNull { pkg in it.blockedApps } ?: return

        // Tell the wall which session it is showing the countdown for.
        prefs.saveSession(blocking.name, blocking.startTime, blocking.endTime)

        startActivity(
            Intent(this, BlockingActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION,
                )
                putExtra(BlockingActivity.EXTRA_PACKAGE, pkg)
            },
        )
    }

    override fun onInterrupt() {}
}
