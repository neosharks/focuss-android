package com.neosharks.focuss

import android.app.Application
import com.neosharks.focuss.data.AppRepository
import com.neosharks.focuss.data.Prefs
import com.neosharks.focuss.data.ScheduleStore

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
