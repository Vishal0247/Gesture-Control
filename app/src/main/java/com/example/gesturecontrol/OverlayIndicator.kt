package com.example.gesturecontrol

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.*

class OverlayIndicator(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var isAdded = false
    private val prefs: SharedPreferences = context.getSharedPreferences("gesture_prefs", Context.MODE_PRIVATE)

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val pillView = PillView(context)
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        val statusBarHeight = getStatusBarHeight()
        y = statusBarHeight + dpToPx(8)
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()

    // COLORS
    private val COLOR_NAVY = Color.parseColor("#EA0A0E14") // 92% alpha
    private val COLOR_BORDER = Color.parseColor("#33FFFFFF")
    private val COLOR_GREY = Color.parseColor("#5B6573")
    private val COLOR_AMBER = Color.parseColor("#E8A94C")
    private val COLOR_TEAL = Color.parseColor("#2DD4A6")
    private val COLOR_VIOLET = Color.parseColor("#8B7CF6")

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "overlayEnabled") {
            if (prefs.getBoolean("overlayEnabled", true) && GestureBus.running.value) attach()
            else detach()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    fun attach() {
        if (!prefs.getBoolean("overlayEnabled", true)) return
        if (isAdded) return
        windowManager.addView(pillView, layoutParams)
        isAdded = true

        scope.launch {
            var lastHandTime = SystemClock.uptimeMillis()
            
            launch {
                GestureBus.trackerError.collect { error ->
                    pillView.trackerError = if (error != null) "Tracker error: $error" else null
                    if (error != null) {
                        pillView.isDotOnly = false
                    }
                }
            }

            launch {
                GestureBus.handPresent.collect { present ->
                    if (present) {
                        lastHandTime = SystemClock.uptimeMillis()
                        pillView.isDotOnly = false
                    }
                }
            }
            
            launch {
                while (isActive) {
                    delay(500)
                    if (GestureBus.trackerError.value != null) {
                        pillView.isDotOnly = false
                    } else if (!GestureBus.handPresent.value) {
                        if (SystemClock.uptimeMillis() - lastHandTime > 3000) {
                            pillView.isDotOnly = true
                        }
                    }
                }
            }

            launch {
                GestureBus.state.collect { gesture ->
                    pillView.gestureName = gesture.name.replace("_", " ")
                }
            }
            
            launch {
                GestureBus.holdProgress.collect { progress ->
                    if (progress != null) {
                        pillView.holdLabel = progress.label
                        pillView.progress = progress.fraction
                    } else {
                        pillView.holdLabel = null
                        pillView.progress = 0f
                    }
                }
            }

            launch {
                GestureBus.actionEvents.collect { action ->
                    pillView.showAction(action)
                }
            }

            launch {
                while (isActive) {
                    val color = determineColor()
                    pillView.setDotColor(color)
                    delay(50) // 20 fps throttle
                }
            }
        }
    }

    private fun determineColor(): Int {
        if (GestureBus.trackerError.value != null) return Color.RED
        if (!GestureBus.handPresent.value) return COLOR_GREY
        val callState = GestureBus.callState.value
        if (callState == CallState.RINGING) {
            val pulse = (SystemClock.uptimeMillis() % 1000) / 1000f
            return if (pulse < 0.5f) COLOR_AMBER else COLOR_TEAL
        }
        if (callState == CallState.IN_CALL) return COLOR_TEAL
        
        val armed = GestureBus.armed.value
        if (!armed) return COLOR_AMBER
        
        val media = GestureBus.mediaMode.value
        if (media) return COLOR_VIOLET
        
        return COLOR_TEAL
    }

    fun detach() {
        if (!isAdded) return
        try {
            windowManager.removeView(pillView)
        } catch (e: Exception) {}
        isAdded = false
        job.cancelChildren()
    }

    private inner class PillView(context: Context) : View(context) {
        private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_NAVY }
        private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(1).toFloat()
        }
        private val paintDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_GREY }
        private val paintProgress = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(2).toFloat()
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dpToPx(12).toFloat()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        var isDotOnly = false
            set(value) {
                if (field != value) {
                    field = value
                    requestLayout()
                    invalidate()
                }
            }
            
        var trackerError: String? = null
            set(value) { field = value; requestLayout(); invalidate() }

        var gestureName = ""
            set(value) { field = value; invalidate() }
            
        var holdLabel: String? = null
            set(value) { field = value; invalidate() }
            
        var progress = 0f
            set(value) { field = value; invalidate() }

        private var actionText: String? = null
        private var actionAnimator: ValueAnimator? = null
        private var actionScale = 0f

        fun setDotColor(color: Int) {
            if (paintDot.color != color) {
                paintDot.color = color
                invalidate()
            }
        }

        fun showAction(action: String) {
            actionText = action
            actionAnimator?.cancel()
            actionAnimator = ValueAnimator.ofFloat(0f, 1f, 1f, 1f, 0f).apply {
                duration = 1200
                addUpdateListener { anim ->
                    actionScale = anim.animatedValue as Float
                    requestLayout()
                    invalidate()
                }
                start()
            }
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val h = dpToPx(28)
            var w = dpToPx(28) // min width (dot only)
            if (!isDotOnly) {
                val label = trackerError ?: actionText?.takeIf { actionScale > 0 } ?: holdLabel ?: gestureName
                val textW = textPaint.measureText(label)
                w = dpToPx(28) + textW.toInt() + dpToPx(20)
            }
            w = (w * (1f + 0.1f * actionScale)).toInt()
            setMeasuredDimension(w, h)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val r = h / 2f

            // Shadow/Background
            canvas.drawRoundRect(0f, 0f, w, h, r, r, paintBg)
            canvas.drawRoundRect(0f, 0f, w, h, r, r, paintBorder)

            val dotCx = dpToPx(14).toFloat()
            val dotCy = r
            val dotRadius = dpToPx(4).toFloat()
            canvas.drawCircle(dotCx, dotCy, dotRadius, paintDot)

            if (progress > 0f) {
                val rectF = RectF(dotCx - dotRadius - dpToPx(3), dotCy - dotRadius - dpToPx(3),
                                  dotCx + dotRadius + dpToPx(3), dotCy + dotRadius + dpToPx(3))
                canvas.drawArc(rectF, -90f, 360f * progress, false, paintProgress)
            }

            if (!isDotOnly) {
                val label = trackerError ?: actionText?.takeIf { actionScale > 0 } ?: holdLabel ?: gestureName
                if (label.isNotEmpty() && label != "NONE") {
                    val textY = h / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                    val textX = dotCx + dpToPx(8) + (w - dotCx - dpToPx(8)) / 2f
                    canvas.drawText(label, textX, textY, textPaint)
                }
            }
        }
    }
}
