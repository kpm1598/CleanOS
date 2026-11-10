package com.cleanos.storage.data.repositories

import android.content.Context

/** Small persisted state: onboarding, last scan time, SAF folder, swipe session pause/index. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("cleanos_prefs", Context.MODE_PRIVATE)
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboarding_done", false)
        set(v) = prefs.edit().putBoolean("onboarding_done", v).apply()
    var lastScanEpochMs: Long
        get() = prefs.getLong("last_scan_ms", 0L)
        set(v) = prefs.edit().putLong("last_scan_ms", v).apply()
    var safTreeUri: String?
        get() = prefs.getString("saf_tree_uri", null)
        set(v) = prefs.edit().putString("saf_tree_uri", v).apply()
    var swipePaused: Boolean
        get() = prefs.getBoolean("swipe_paused", false)
        set(v) = prefs.edit().putBoolean("swipe_paused", v).apply()
}
