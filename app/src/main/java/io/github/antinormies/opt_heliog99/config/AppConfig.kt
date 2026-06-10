package io.github.antinormies.opt_heliog99.config

import android.content.Context
import android.content.SharedPreferences

class AppConfig(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var profile: Profile
        get() = Profile.fromValue(prefs.getString(PROFILE_KEY, Profile.BALANCED.value)!!)
        set(value) = prefs.edit().putString(PROFILE_KEY, value.value).apply()

    var optimizeVulkan: Boolean
        get() = prefs.getBoolean(VULKAN_KEY, true)
        set(value) = prefs.edit().putBoolean(VULKAN_KEY, value).apply()

    var lastLog: String
        get() = prefs.getString(LOG_KEY, "")!!
        set(value) = prefs.edit().putString(LOG_KEY, value).apply()

    enum class Profile(val value: String) {
        BALANCED("balanced"),
        PERFORMANCE("performance");

        companion object {
            fun fromValue(v: String) = entries.firstOrNull { it.value == v } ?: BALANCED
        }
    }

    companion object {
        private const val PREF_NAME = "opt_heliog99_config"
        private const val PROFILE_KEY = "profile"
        private const val VULKAN_KEY = "optimize_vulkan"
        private const val LOG_KEY = "last_log"
    }
}
