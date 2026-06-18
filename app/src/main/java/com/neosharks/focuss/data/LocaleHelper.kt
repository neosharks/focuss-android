package com.neosharks.focuss.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the user's chosen in-app language by wrapping a base context with an
 * overridden locale. Activities call [wrap] from attachBaseContext, so the whole
 * resource lookup (strings + layout direction for RTL) uses the selected language
 * regardless of the system locale. Works on every API level — no AppCompat needed.
 */
object LocaleHelper {

    fun wrap(context: Context): Context {
        val tag = Prefs(context).appLanguage
        if (tag.isEmpty()) return context

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
