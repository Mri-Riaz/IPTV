package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("novastream_iptv_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "LIGHT", "DARK"
        private const val KEY_THEME_COLOR = "theme_color" // "COSMIC_BLUE", "MYSTIC_PURPLE", "SUNSET_ORANGE", "MINT_GREEN"
        private const val KEY_BUFFERING_OPTION = "buffering_option" // "LOW", "NORMAL", "HIGH"
        private const val KEY_PARENTAL_PIN = "parental_pin"
        private const val KEY_PARENTAL_ENABLED = "parental_enabled"
        private const val KEY_APP_LANGUAGE = "app_language" // "en", "es", "fr"
        private const val KEY_REMINDERS = "epg_reminders"
        private const val KEY_EPG_URL = "epg_url"
    }

    var epgUrl: String
        get() = prefs.getString(KEY_EPG_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EPG_URL, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "DARK") ?: "DARK"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var themeColor: String
        get() = prefs.getString(KEY_THEME_COLOR, "COSMIC_BLUE") ?: "COSMIC_BLUE"
        set(value) = prefs.edit().putString(KEY_THEME_COLOR, value).apply()

    var bufferingOption: String
        get() = prefs.getString(KEY_BUFFERING_OPTION, "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString(KEY_BUFFERING_OPTION, value).apply()

    var parentalPin: String
        get() = prefs.getString(KEY_PARENTAL_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PARENTAL_PIN, value).apply()

    var parentalEnabled: Boolean
        get() = prefs.getBoolean(KEY_PARENTAL_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PARENTAL_ENABLED, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var reminders: Set<String>
        get() = prefs.getStringSet(KEY_REMINDERS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_REMINDERS, value).apply()

    fun isReminderSet(key: String): Boolean {
        return reminders.contains(key)
    }

    fun toggleReminder(key: String): Boolean {
        val current = reminders.toMutableSet()
        val isAdded = if (current.contains(key)) {
            current.remove(key)
            false
        } else {
            current.add(key)
            true
        }
        reminders = current
        return isAdded
    }

    fun verifyPin(pin: String): Boolean {
        return parentalPin == pin
    }
}
