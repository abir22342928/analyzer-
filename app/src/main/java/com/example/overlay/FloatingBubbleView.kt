package com.example.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.model.BubbleState
import com.example.model.SignalType
import com.example.trade.AutoTradeEngine
import kotlin.math.abs
import kotlin.math.min

class FloatingBubbleView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private val onTap: () -> Unit,
    private val onLongPress: () -> Unit
) : View(context) {

    var state: BubbleState = BubbleState.READY
        set(value) {
            field = value
            updateStateAnimation()
            invalidate()
        }

    var lastSignal: SignalType? = null
        set(value) {
            field = value
            invalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0F172A")
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#00E676")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#4D00E676")
    }

    private var pulseRadius = 0f
    private var pulseAlpha = 0
    private var pulseAnimator: ValueAnimator? = null

    // Touch tracking
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var hasFiredLongPress = false

    private val longPressRunnable = Runnable {
        if (!isDragging) {
            hasFiredLongPress = true
            onLongPress()
        }
    }

    init {
        updateStateAnimation()
    }

    private fun updateStateAnimation() {
        pulseAnimator?.cancel()
        if (state == BubbleState.ANALYZING) {
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val progress = it.animatedValue as Float
                    pulseRadius = 16f * progress
                    pulseAlpha = ((1f - progress) * 200).toInt()
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                hasFiredLongPress = false
                longPressHandler.postDelayed(longPressRunnable, 600)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (abs(dx) > 12 || abs(dy) > 12) {
                    isDragging = true
                    longPressHandler.removeCallbacks(longPressRunnable)
                    layoutParams.x = initialX + dx
                    layoutParams.y = initialY + dy
                    try {
                        windowManager.updateViewLayout(this, layoutParams)
                    } catch (e: Exception) {
                        // ignore layout detached race conditions
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                if (!isDragging && !hasFiredLongPress) {
                    onTap()
                }
                isDragging = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 10f

        // Animated pulse in analyzing state
        if (state == BubbleState.ANALYZING && pulseRadius > 0) {
            pulsePaint.color = Color.argb(pulseAlpha, 0, 230, 118)
            canvas.drawCircle(cx, cy, radius + pulseRadius, pulsePaint)
        }

        // Background
        bgPaint.color = when (state) {
            BubbleState.READY -> Color.parseColor("#0F172A")
            BubbleState.ANALYZING -> Color.parseColor("#064E3B")
            BubbleState.RESULT -> Color.parseColor("#0F172A")
            BubbleState.ERROR -> Color.parseColor("#450A0A")
            BubbleState.STOPPED -> Color.parseColor("#1E293B")
        }
        canvas.drawCircle(cx, cy, radius, bgPaint)

        // Border color
        borderPaint.color = when (state) {
            BubbleState.READY -> Color.parseColor("#00E676")
            BubbleState.ANALYZING -> Color.parseColor("#00E5FF")
            BubbleState.RESULT -> when (lastSignal) {
                SignalType.UP -> Color.parseColor("#00E676")
                SignalType.DOWN -> Color.parseColor("#FF5252")
                null -> Color.parseColor("#00E676")
            }
            BubbleState.ERROR -> Color.parseColor("#FF5252")
            BubbleState.STOPPED -> Color.parseColor("#64748B")
        }
        canvas.drawCircle(cx, cy, radius, borderPaint)

        // Text & icons
        textPaint.textSize = radius * 0.48f
        val yOffset = (textPaint.descent() + textPaint.ascent()) / 2

        when (state) {
            BubbleState.READY -> {
                textPaint.color = Color.parseColor("#00E676")
                canvas.drawText("AI", cx, cy - yOffset, textPaint)
            }
            BubbleState.ANALYZING -> {
                textPaint.color = Color.parseColor("#00E5FF")
                canvas.drawText("SCAN", cx, cy - yOffset, textPaint)
            }
            BubbleState.RESULT -> {
                val badgeText = when (lastSignal) {
                    SignalType.UP -> "UP ▲"
                    SignalType.DOWN -> "DN ▼"
                    null -> "AI"
                }
                textPaint.color = borderPaint.color
                canvas.drawText(badgeText, cx, cy - yOffset, textPaint)
            }
            BubbleState.ERROR -> {
                textPaint.color = Color.parseColor("#FF5252")
                canvas.drawText("ERR", cx, cy - yOffset, textPaint)
            }
            BubbleState.STOPPED -> {
                textPaint.color = Color.parseColor("#94A3B8")
                canvas.drawText("OFF", cx, cy - yOffset, textPaint)
            }
        }

        // If Auto Trade Bot is active, draw a glowing badge dot in top-right corner
        if (AutoTradeEngine.config.value.enabled) {
            val autoTradeDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00E676")
                style = Paint.Style.FILL
            }
            val dotX = cx + radius * 0.65f
            val dotY = cy - radius * 0.65f
            canvas.drawCircle(dotX, dotY, 9f, autoTradeDotPaint)

            val autoTradeDotBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawCircle(dotX, dotY, 9f, autoTradeDotBorder)
        }
    }
}
