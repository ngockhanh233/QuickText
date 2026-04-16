package com.quicktext.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

    // -------- Swipe state --------
    private var swipeStartX = 0f
    private var swipeStartY = 0f
    private var swiping = false
    private val swipePath = Path()
    private val swipeLetters = StringBuilder()
    private val swipeThresholdPx: Float =
        ViewConfiguration.get(context).scaledTouchSlop * 1.5f

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
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!swiping) {
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
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_UP -> {
                if (swiping) {
                    val letters = swipeLetters.toString()
                    endSwiping()
                    onSwipeComplete?.invoke(letters)
                    return true
                }
                return super.onTouchEvent(me)
            }

            MotionEvent.ACTION_CANCEL -> {
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
        swiping = true
        // Cancel the pending key tap so the first letter isn't committed.
        val cancel = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        super.onTouchEvent(cancel)
        cancel.recycle()
        // Suppress preview popups during the swipe.
        isPreviewEnabled = false

        swipePath.reset()
        swipePath.moveTo(swipeStartX, swipeStartY)
        recordKeyAt(swipeStartX.toInt(), swipeStartY.toInt())
        invalidate()
    }

    private fun endSwiping() {
        swiping = false
        swipePath.reset()
        swipeLetters.setLength(0)
        isPreviewEnabled = true
        invalidate()
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
    }
}
