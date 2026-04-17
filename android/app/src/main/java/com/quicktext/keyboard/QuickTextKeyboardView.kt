package com.quicktext.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration

/**
 * Custom [KeyboardView] that:
 *   1. Hides "placeholder" keys whose primary code is 0 (margin spacers).
 *   2. Exposes a long-press callback for specific keys.
 *   3. Detects glide / swipe-to-type gestures on letter keys, draws the trail,
 *      and reports the visited letter sequence via [onSwipeComplete].
 *   4. Draws its OWN key preview bubble directly on the canvas (no PopupWindow),
 *      so the bubble appears/disappears instantly with no animation.
 */
class QuickTextKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : KeyboardView(context, attrs, defStyleAttr) {

    /** Long-press callback. Return `true` to consume the long-press. */
    var onKeyLongPress: ((code: Int) -> Boolean)? = null

    /** Fired on swipe release with the de-duplicated sequence of letters visited. */
    var onSwipeComplete: ((letters: String) -> Unit)? = null

    /** Background colour painted over placeholder keys to hide them. */
    var placeholderOverColor: Int = Color.parseColor("#E4E7EB")

    /** Whether glide/swipe-to-type is enabled. */
    var glideEnabled: Boolean = true

    /** User preference: whether to show the custom key preview bubble. */
    var userPreviewEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pressedKeyForPreview = null
                invalidate()
            }
        }

    private val density = context.resources.displayMetrics.density

    // -------- Paints --------
    private val overlayPaint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }

    private val swipePaint = Paint().apply {
        color = Color.parseColor("#4A90D9")
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        alpha = 200
        isAntiAlias = true
    }

    private val previewBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val previewBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0D4D9")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    private val previewTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A1A")
        textAlign = Paint.Align.CENTER
        textSize = 30f * density
    }

    // -------- Swipe state --------
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swiping = false
    private val swipePath = Path()
    private val swipeLetters = StringBuilder()
    private val swipeThresholdPx: Float =
        ViewConfiguration.get(context).scaledTouchSlop * 1.5f

    // -------- Custom preview state --------
    private var pressedKeyForPreview: Keyboard.Key? = null

    init {
        // Always disable the built-in preview popup. We draw our own preview in onDraw
        // so there is no PopupWindow translation animation between key presses.
        isPreviewEnabled = false
    }

    override fun onLongPress(popupKey: Keyboard.Key?): Boolean {
        val code = popupKey?.codes?.firstOrNull() ?: return super.onLongPress(popupKey)
        val handled = onKeyLongPress?.invoke(code) ?: false
        if (handled) return true
        return super.onLongPress(popupKey)
    }

    override fun onTouchEvent(me: MotionEvent): Boolean {
        when (me.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartX = me.x
                swipeStartY = me.y
                swiping = false
                swipePath.reset()
                swipeLetters.setLength(0)

                // Ignore touches on placeholder spacer keys.
                val k = findKeyAt(me.x.toInt(), me.y.toInt())
                if (k == null || k.codes?.firstOrNull() == 0) {
                    return false
                }
                updatePreviewKey(me.x, me.y)
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!swiping && glideEnabled) {
                    val dx = me.x - swipeStartX
                    val dy = me.y - swipeStartY
                    if (dx * dx + dy * dy > swipeThresholdPx * swipeThresholdPx) {
                        // Movement exceeds slop -> enter glide mode.
                        startSwiping()
                    }
                }
                if (swiping) {
                    swipePath.lineTo(me.x, me.y)
                    recordKeyAt(me.x.toInt(), me.y.toInt())
                    invalidate()
                    return true
                }
                // Update which key is being previewed as the finger slides.
                updatePreviewKey(me.x, me.y)
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_UP -> {
                clearPreviewKey()
                if (swiping) {
                    val letters = swipeLetters.toString()
                    endSwiping()
                    onSwipeComplete?.invoke(letters)
                    return true
                }
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_CANCEL -> {
                clearPreviewKey()
                if (swiping) {
                    endSwiping()
                    return true
                }
                return super.onTouchEvent(me)
            }
        }
        return super.onTouchEvent(me)
    }

    private fun startSwiping() {
        // Clear the static preview - we are now in swipe mode.
        clearPreviewKey()
        swiping = true
        // Cancel the pending key tap so the first letter isn't committed.
        val cancel = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        super.onTouchEvent(cancel)
        cancel.recycle()

        swipePath.reset()
        swipePath.moveTo(swipeStartX, swipeStartY)
        recordKeyAt(swipeStartX.toInt(), swipeStartY.toInt())
        invalidate()
    }

    private fun endSwiping() {
        swiping = false
        swipePath.reset()
        swipeLetters.setLength(0)
        invalidate()
    }

    private fun updatePreviewKey(x: Float, y: Float) {
        if (!userPreviewEnabled) return
        val k = findKeyAt(x.toInt(), y.toInt())
        val newKey = if (k != null && shouldShowPreview(k)) k else null
        if (newKey !== pressedKeyForPreview) {
            pressedKeyForPreview = newKey
            invalidate()
        }
    }

    private fun clearPreviewKey() {
        if (pressedKeyForPreview != null) {
            pressedKeyForPreview = null
            invalidate()
        }
    }

    private fun shouldShowPreview(k: Keyboard.Key): Boolean {
        val code = k.codes?.firstOrNull() ?: return false
        // Skip placeholder, control keys (negative codes), and the space bar
        // (whose label is "Vi"/"En" and would look noisy as a bubble).
        if (code <= 0 || code == 32) return false
        // Only printable ASCII (letters, digits, punctuation).
        if (code !in 33..126) return false
        // Skip keys without a visible label.
        return !k.label.isNullOrEmpty()
    }

    private fun recordKeyAt(x: Int, y: Int) {
        val k = findKeyAt(x, y) ?: return
        val code = k.codes?.firstOrNull() ?: return
        if (code in 'a'.code..'z'.code) {
            val c = code.toChar()
            if (swipeLetters.isEmpty() || swipeLetters.last() != c) {
                swipeLetters.append(c)
            }
        }
    }

    private fun findKeyAt(x: Int, y: Int): Keyboard.Key? {
        val keys = keyboard?.keys ?: return null
        for (k in keys) {
            if (k.isInside(x, y)) return k
        }
        return null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 1. Hide placeholder keys by painting over them.
        val keys = keyboard?.keys ?: return
        overlayPaint.color = placeholderOverColor
        for (key in keys) {
            if (key.codes?.firstOrNull() == 0) {
                canvas.drawRect(
                    key.x.toFloat(),
                    key.y.toFloat(),
                    (key.x + key.width).toFloat(),
                    (key.y + key.height).toFloat(),
                    overlayPaint,
                )
            }
        }
        // 2. Draw glide trail on top.
        if (swiping && !swipePath.isEmpty) {
            canvas.drawPath(swipePath, swipePaint)
        }
        // 3. Draw the custom key preview bubble for the currently pressed key.
        if (userPreviewEnabled && !swiping) {
            pressedKeyForPreview?.let { drawKeyPreview(canvas, it) }
        }
    }

    private fun drawKeyPreview(canvas: Canvas, key: Keyboard.Key) {
        val label = key.label?.toString() ?: return

        val minWidth = 56f * density
        val previewWidth = maxOf(key.width.toFloat() * 1.15f, minWidth)
        val previewHeight = key.height.toFloat() + 12f * density

        val cx = key.x + key.width / 2f
        val left = cx - previewWidth / 2f
        val right = cx + previewWidth / 2f

        // Bottom of preview overlaps the top portion of the pressed key for a connected look.
        var bottom = key.y.toFloat() + key.height * 0.2f
        var top = bottom - previewHeight
        // Clamp so the preview never draws outside the keyboard view.
        if (top < 0f) {
            top = 0f
            bottom = top + previewHeight
        }

        val rect = RectF(left, top, right, bottom)
        val radius = 10f * density
        canvas.drawRoundRect(rect, radius, radius, previewBgPaint)
        canvas.drawRoundRect(rect, radius, radius, previewBorderPaint)

        // Center label vertically in the preview bubble (slightly above key overlap).
        val visualCenterY = top + (bottom - key.height * 0.2f - top) / 2f
        val textY = visualCenterY - (previewTextPaint.descent() + previewTextPaint.ascent()) / 2f
        canvas.drawText(label, cx, textY, previewTextPaint)
    }
}
