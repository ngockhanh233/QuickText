package com.quicktext.keyboard

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed settings for the QuickText IME. Defaults match the
 * out-of-the-box experience (everything on).
 */
class KeyboardSettings(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    var glideEnabled: Boolean
        get() = prefs.getBoolean(KEY_GLIDE, true)
        set(value) = prefs.edit().putBoolean(KEY_GLIDE, value).apply()

    var keyPreviewEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREVIEW, true)
        set(value) = prefs.edit().putBoolean(KEY_PREVIEW, value).apply()

    var autoCapsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOCAPS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOCAPS, value).apply()

    companion object {
        private const val PREFS_NAME = "quicktext_keyboard_prefs"
        private const val KEY_GLIDE = "glide_enabled"
        private const val KEY_PREVIEW = "key_preview_enabled"
        private const val KEY_AUTOCAPS = "auto_caps_enabled"
    }
}
