package com.quicktext

import android.content.Intent
import android.os.Build
import android.provider.Settings
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

    @ReactMethod
    fun openAccessibilitySettings() {
        val context = reactApplicationContext
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
