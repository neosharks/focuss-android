package com.focuss

import android.app.Application
import com.focuss.data.AppRepository
import com.focuss.data.Prefs
import com.focuss.data.ScheduleStore

/**
 * Application singleton. Holds the lightweight, stateless backend objects so the
 * UI layer (and the ViewModel) can share one instance each — no DI framework
 * needed for an app this size.
 */
class FocussApp : Application() {

    val scheduleStore: ScheduleStore by lazy { ScheduleStore(this) }
    val appRepository: AppRepository by lazy { AppRepository(this) }
    val prefs: Prefs by lazy { Prefs(this) }
}
