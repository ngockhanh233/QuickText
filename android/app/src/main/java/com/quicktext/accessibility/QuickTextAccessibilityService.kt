package com.quicktext.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.LinearLayout
import android.widget.TextView
import com.quicktext.R

/**
 * Accessibility service that shows a floating suggestion bar on top of ANY
 * keyboard (Gboard, SwiftKey, etc.). When the user types a few characters that
 * match a saved phrase shortcut, the bar appears with suggestions.
 *
 * The overlay uses TYPE_ACCESSIBILITY_OVERLAY so no SYSTEM_ALERT_WINDOW
 * permission is needed — just enabling the accessibility service is enough.
 *
 * When the QuickText custom keyboard (IME) is already active, this service
 * stays quiet so the two suggestion UIs don't duplicate each other.
 */
class QuickTextAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var suggestionBar: LinearLayout? = null
    private var isOverlayShowing = false
    private lateinit var phrasesDb: PhrasesDatabase
    /** Timestamp of our last addView/removeView so we can ignore the window events we cause ourselves. */
    private var lastOverlayChangeMs = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        phrasesDb = PhrasesDatabase(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Skip window events triggered by our own overlay add/remove
                // (they arrive within ~50ms). After that cooldown, check
                // whether the keyboard is actually gone.
                if (System.currentTimeMillis() - lastOverlayChangeMs < 100) return
                if (!isKeyboardVisible()) hideOverlay()
                return
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // If our own QuickText keyboard is active, let the IME handle suggestions.
                if (isQuickTextKeyboardActive()) {
                    hideOverlay()
                    return
                }

                val text = event.text?.joinToString("") ?: ""
                if (text.isBlank()) {
                    hideOverlay()
                    return
                }

                // Extract last word from the typed text.
                val lastWord = text.trim().split(Regex("\\s+")).lastOrNull() ?: ""
                if (lastWord.length < 2) {
                    hideOverlay()
                    return
                }

                val matches = phrasesDb.searchPhrases(lastWord, limit = 6)
                if (matches.isEmpty()) {
                    hideOverlay()
                } else {
                    showOverlay(matches, event.source)
                }
            }
        }
    }

    /**
     * Returns true if an IME window is currently visible on screen.
     */
    private fun isKeyboardVisible(): Boolean {
        return try {
            val wins = windows ?: return false
            wins.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        } catch (_: Exception) {
            false
        }
    }

    override fun onInterrupt() {
        hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
    }

    private fun isQuickTextKeyboardActive(): Boolean {
        val currentIme = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        )
        return currentIme?.contains("com.quicktext") == true
    }

    // ---------- Overlay management ----------

    private fun showOverlay(phrases: List<Phrase>, sourceNode: AccessibilityNodeInfo?) {
        ensureOverlayCreated()
        val bar = suggestionBar ?: return
        bar.removeAllViews()

        val inflater = LayoutInflater.from(this)
        for (phrase in phrases) {
            val chip = inflater.inflate(R.layout.suggestion_item, bar, false)
            chip.findViewById<TextView>(R.id.shortcut).text = phrase.shortcut
            chip.findViewById<TextView>(R.id.text).text = phrase.text
            chip.setOnClickListener {
                insertPhrase(phrase)
                hideOverlay()
            }
            bar.addView(chip)
        }

        if (!isOverlayShowing) {
            try {
                windowManager?.addView(overlayView, createLayoutParams())
                isOverlayShowing = true
                lastOverlayChangeMs = System.currentTimeMillis()
            } catch (_: Exception) {
                // View might already be attached.
            }
        } else {
            // Update position in case keyboard height changed.
            try {
                overlayView?.let { windowManager?.updateViewLayout(it, createLayoutParams()) }
            } catch (_: Exception) {
            }
        }
    }

    private fun hideOverlay() {
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {
                // Already removed or never added.
            }
            isOverlayShowing = false
            lastOverlayChangeMs = System.currentTimeMillis()
        }
    }

    private fun ensureOverlayCreated() {
        if (overlayView != null) return
        overlayView = LayoutInflater.from(this).inflate(R.layout.floating_suggestion, null)
        suggestionBar = overlayView?.findViewById(R.id.floating_bar)
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val barHeight = (48 * density).toInt()
        val kbHeight = getKeyboardHeight()
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            barHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
            y = kbHeight
        }
    }

    /**
     * Detect the height of the currently visible IME window via the accessibility
     * windows list. Falls back to a reasonable default (260dp) if the IME window
     * is not found.
     */
    private fun getKeyboardHeight(): Int {
        val fallback = (260 * resources.displayMetrics.density).toInt()
        return try {
            val wins = windows ?: return fallback
            for (w in wins) {
                if (w.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    val rect = Rect()
                    w.getBoundsInScreen(rect)
                    return rect.height()
                }
            }
            fallback
        } catch (_: Exception) {
            fallback
        }
    }

    // ---------- Text insertion ----------

    /**
     * Replaces the last word (the shortcut the user typed) with the full phrase
     * text in the currently focused text field.
     */
    private fun insertPhrase(phrase: Phrase) {
        val rootNode = try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        } ?: return
        val focused = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: run {
            rootNode.recycle()
            return
        }

        val currentText = focused.text?.toString() ?: ""
        // Find last word boundary and replace the shortcut with the phrase text.
        val lastSpaceIndex = currentText.lastIndexOf(' ')
        val prefix = if (lastSpaceIndex >= 0) currentText.substring(0, lastSpaceIndex + 1) else ""
        val newText = prefix + phrase.text + " "

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText,
            )
        }
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        focused.recycle()
        rootNode.recycle()
    }
}
