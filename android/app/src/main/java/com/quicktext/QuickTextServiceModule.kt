package com.quicktext

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class QuickTextServiceModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "QuickTextService"

    @ReactMethod
    fun startService() {
        val context = reactApplicationContext
        val intent = Intent(context, QuickTextForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    @ReactMethod
    fun stopService() {
        val context = reactApplicationContext
        val intent = Intent(context, QuickTextForegroundService::class.java)
        context.stopService(intent)
    }

    /**
     * Opens the system "Languages & input" settings screen where the user can enable
     * the QuickText keyboard in the list of input methods.
     */
    @ReactMethod
    fun openKeyboardSettings() {
        val context = reactApplicationContext
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Shows the IME picker so the user can switch the active keyboard to QuickText.
     * Works only when the soft keyboard is showing or an input field is focused.
     */
    @ReactMethod
    fun showKeyboardPicker() {
        val context = reactApplicationContext
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    /**
     * Returns true if the QuickText keyboard is enabled in system settings.
     */
    @ReactMethod
    fun isKeyboardEnabled(promise: Promise) {
        try {
            val context = reactApplicationContext
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val enabled = imm.enabledInputMethodList.any { info ->
                info.packageName == context.packageName
            }
            promise.resolve(enabled)
        } catch (e: Exception) {
            promise.reject("E_CHECK_IME", e)
        }
    }
}
