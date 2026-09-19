package com.example.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.model.MarketAnalysisResult
import com.example.model.OrderStatus
import com.example.model.SignalType
import com.example.trade.AutoTradeEngine

class AnalyzerOverlayView(
    context: Context,
    private val onCancel: () -> Unit,
    private val onAnalyzeAgain: () -> Unit,
    private val onClose: () -> Unit,
    private val onOpenSimulator: () -> Unit = {}
) : FrameLayout(context) {

    private val scannerCanvasView: ScannerCanvasView
    private val contentContainer: FrameLayout
    private val handler = Handler(Looper.getMainLooper())
    private var scanStepRunnable: Runnable? = null

    fun setInvisibleForCapture(invisible: Boolean) {
        visibility = if (invisible) View.INVISIBLE else View.VISIBLE
    }

    init {
        setBackgroundColor(Color.parseColor("#B3050B14")) // 70% dark scrim

        contentContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        addView(contentContainer)

        scannerCanvasView = ScannerCanvasView(context)
        contentContainer.addView(scannerCanvasView)

        showScanningState()
    }

    fun showScanningState() {
        contentContainer.removeAllViews()
        contentContainer.addView(scannerCanvasView)
        scannerCanvasView.startScan()

        val steps = listOf(
            "আমি এখন ট্রেডিং চার্ট দেখতে পাচ্ছি এবং এনালাইসিস করছি...",
            "ক্যান্ডেলস্টিক প্যাটার্ন স্ক্যান করা হচ্ছে...",
            "বায়ার্স ও সেলার্স ভলিউম প্রেশার বিশ্লেষণ...",
            "AI ব্রেন পরবর্তী ক্যান্ডেল (UP/DOWN) নির্ধারণ করছে..."
        )

        val statusCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 36, 48, 36)
        }

        val titleTv = TextView(context).apply {
            text = "AI TRADING ANALYZER"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val stepTv = TextView(context).apply {
            text = steps[0]
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        val cancelBtn = TextView(context).apply {
            text = "[ CANCEL ]"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(24, 12, 24, 12)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FF5252"))
                cornerRadius = 12f
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                stopAnimation()
                onCancel()
            }
        }

        statusCard.addView(titleTv)
        statusCard.addView(stepTv)
        statusCard.addView(cancelBtn)

        val cardWrap = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F20F172A"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#334155"))
            }
            elevation = 16f
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                setMargins(48, 0, 48, 0)
            }
            addView(statusCard)
        }

        contentContainer.addView(cardWrap)

        // Progressively advance status text
        var currentStep = 0
        scanStepRunnable = object : Runnable {
            override fun run() {
                currentStep++
                if (currentStep < steps.size) {
                    stepTv.text = steps[currentStep]
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.postDelayed(scanStepRunnable!!, 500)
    }

    fun showResult(result: MarketAnalysisResult) {
        stopAnimation()
        contentContainer.removeAllViews()

        val scrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                setMargins(32, 48, 32, 48)
            }
        }

        val cardView = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F80F172A"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#334155"))
            }
            elevation = 20f
        }

        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 36, 40, 36)
        }

        // Subtitle & Target
        val targetTv = TextView(context).apply {
            text = "NEXT CANDLE ANALYSIS"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
        }
        rootLayout.addView(targetTv)

        // Signal Badge
        val signalColor = when (result.signal) {
            SignalType.UP -> Color.parseColor("#00E676")
            SignalType.DOWN -> Color.parseColor("#FF5252")
        }

        val signalBadge = TextView(context).apply {
            text = "${result.signal.badge} ${result.signal.title}"
            setTextColor(signalColor)
            textSize = 26f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        rootLayout.addView(signalBadge)

        // Score
        val scoreTv = TextView(context).apply {
            text = "Analysis Score: ${result.score}/100"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        rootLayout.addView(scoreTv)

        // Bengali Next Candle Prediction & AI Brain Banner
        val bengaliPredictionCard = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            background = GradientDrawable().apply {
                setColor(if (result.signal == SignalType.UP) Color.parseColor("#1A00E676") else Color.parseColor("#1AFF5252"))
                cornerRadius = 14f
                setStroke(2, if (result.signal == SignalType.UP) Color.parseColor("#00E676") else Color.parseColor("#FF5252"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 14)
            }
        }

        val bnVerdictTv = TextView(context).apply {
            text = if (result.nextCandleBengali.isNotBlank()) result.nextCandleBengali else if (result.signal == SignalType.UP) "পরবর্তী ক্যান্ডেল: আপ (UP) 🟢" else "পরবর্তী ক্যান্ডেল: ডাউন (DOWN) 🔴"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        bengaliPredictionCard.addView(bnVerdictTv)

        if (result.aiBrainInsight.isNotBlank()) {
            val aiBrainTv = TextView(context).apply {
                text = "🧠 ${result.aiBrainInsight}"
                setTextColor(Color.parseColor("#CBD5E1"))
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 8, 0, 0)
                lineSpacingExtra = 4f
            }
            bengaliPredictionCard.addView(aiBrainTv)
        }

        rootLayout.addView(bengaliPredictionCard)

        // Auto Trade status badge if enabled
        val autoTradeCfg = AutoTradeEngine.config.value
        if (autoTradeCfg.enabled) {
            val lastTrade = AutoTradeEngine.lastExecutedTrade.value
            val isWin = lastTrade?.status == OrderStatus.CLOSED_WIN
            val autoTradeBanner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 12, 16, 12)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(if (isWin) "#1A00E676" else "#1AFF5252"))
                    cornerRadius = 10f
                    setStroke(2, Color.parseColor(if (isWin) "#00E676" else "#FF5252"))
                }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, 16)
                }
            }

            val autoTradeTitle = TextView(context).apply {
                text = "⚡ AUTO TRADE EXECUTED"
                setTextColor(Color.parseColor(if (isWin) "#00E676" else "#FF5252"))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            val autoTradeDetail = TextView(context).apply {
                val pnlText = if (lastTrade != null) "PnL: ${if (lastTrade.pnl >= 0) "+$" else "-$"}${kotlin.math.abs(lastTrade.pnl)}" else "Order Filled"
                text = "$pnlText • Size: $${autoTradeCfg.tradeAmount} • Entry: ${lastTrade?.entryPrice ?: 0.0}"
                setTextColor(Color.WHITE)
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 0)
            }
            autoTradeBanner.addView(autoTradeTitle)
            autoTradeBanner.addView(autoTradeDetail)
            rootLayout.addView(autoTradeBanner)
        }

        // Separator
        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2).apply {
                setMargins(0, 0, 0, 16)
            }
            setBackgroundColor(Color.parseColor("#334155"))
        }
        rootLayout.addView(divider)

        // Metrics Table
        fun addMetricRow(label: String, value: String) {
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val labelTv = TextView(context).apply {
                text = "$label:"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val valueTv = TextView(context).apply {
                text = value
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            row.addView(labelTv)
            row.addView(valueTv)
            rootLayout.addView(row)
        }

        addMetricRow("Asset / Chart", result.asset)
        addMetricRow("Timeframe", result.timeframe)
        addMetricRow("Trend", result.trend)
        addMetricRow("Momentum", result.momentum)
        addMetricRow("Structure", result.structure)
        addMetricRow("Pattern", result.pattern)
        addMetricRow("S / R Level", result.supportResistance)

        // Reasons
        if (result.reasons.isNotEmpty()) {
            val reasonsHeader = TextView(context).apply {
                text = "Analysis Reasons:"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 16, 0, 6)
            }
            rootLayout.addView(reasonsHeader)

            for (reason in result.reasons) {
                val reasonItem = TextView(context).apply {
                    text = "✓ $reason"
                    setTextColor(Color.parseColor("#E2E8F0"))
                    textSize = 12f
                    setPadding(0, 2, 0, 2)
                }
                rootLayout.addView(reasonItem)
            }
        }

        // Risk Factors
        if (result.riskFactors.isNotEmpty()) {
            val riskHeader = TextView(context).apply {
                text = "Risk Factors:"
                setTextColor(Color.parseColor("#F59E0B"))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 12, 0, 6)
            }
            rootLayout.addView(riskHeader)

            for (rf in result.riskFactors) {
                val rfItem = TextView(context).apply {
                    text = "! $rf"
                    setTextColor(Color.parseColor("#CBD5E1"))
                    textSize = 12f
                    setPadding(0, 2, 0, 2)
                }
                rootLayout.addView(rfItem)
            }
        }

        // Disclaimer
        val disclaimerTv = TextView(context).apply {
            text = result.disclaimer
            setTextColor(Color.parseColor("#64748B"))
            textSize = 10f
            setPadding(0, 16, 0, 20)
            gravity = Gravity.CENTER
        }
        rootLayout.addView(disclaimerTv)

        // Button row
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val reAnalyzeBtn = TextView(context).apply {
            text = "[ ANALYZE AGAIN ]"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(28, 16, 28, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2200E676"))
                cornerRadius = 12f
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 12, 0)
            }
            gravity = Gravity.CENTER
            setOnClickListener {
                showScanningState()
                onAnalyzeAgain()
            }
        }

        val closeBtn = TextView(context).apply {
            text = "[ CLOSE ]"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(28, 16, 28, 16)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 12f
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(12, 0, 0, 0)
            }
            gravity = Gravity.CENTER
            setOnClickListener {
                onClose()
            }
        }

        btnRow.addView(reAnalyzeBtn)
        btnRow.addView(closeBtn)
        rootLayout.addView(btnRow)

        cardView.addView(rootLayout)
        scrollView.addView(cardView)
        contentContainer.addView(scrollView)
    }

    fun showError(message: String) {
        stopAnimation()
        contentContainer.removeAllViews()

        val cardView = FrameLayout(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F80F172A"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#450A0A"))
            }
            elevation = 16f
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                setMargins(40, 0, 40, 0)
            }
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 36, 48, 36)
        }

        val iconTv = TextView(context).apply {
            text = "⚠️"
            textSize = 36f
            gravity = Gravity.CENTER
        }

        val titleTv = TextView(context).apply {
            text = "ট্রেডিং চার্ট ডিটেক্ট হয়নি"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 17f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 8)
        }

        val msgTv = TextView(context).apply {
            text = message
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val btnColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val simulatorBtn = TextView(context).apply {
            text = "▶  OPEN CHART SIMULATOR"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(28, 14, 28, 14)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1A00E5FF"))
                cornerRadius = 12f
                setStroke(2, Color.parseColor("#00E5FF"))
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
            setOnClickListener {
                onClose()
                onOpenSimulator()
            }
        }

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val retryBtn = TextView(context).apply {
            text = "[ TRY AGAIN ]"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(24, 14, 24, 14)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#2200E676"))
                cornerRadius = 12f
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                onAnalyzeAgain()
            }
        }

        val closeBtn = TextView(context).apply {
            text = "[ CLOSE ]"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(24, 14, 24, 14)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#334155"))
                cornerRadius = 12f
            }
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(12, 0, 0, 0)
            }
            setOnClickListener {
                onClose()
            }
        }

        btnRow.addView(retryBtn)
        btnRow.addView(closeBtn)
        btnColumn.addView(simulatorBtn)
        btnColumn.addView(btnRow)

        layout.addView(iconTv)
        layout.addView(titleTv)
        layout.addView(msgTv)
        layout.addView(btnColumn)
        cardView.addView(layout)
        contentContainer.addView(cardView)
    }

    private fun stopAnimation() {
        scannerCanvasView.stopScan()
        scanStepRunnable?.let { handler.removeCallbacks(it) }
    }

    /**
     * Futuristic laser scan line and glowing corner brackets view
     */
    private class ScannerCanvasView(context: Context) : View(context) {
        private val laserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4f
            color = Color.parseColor("#00E676")
        }

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f
            color = Color.parseColor("#00E676")
            style = Paint.Style.STROKE
        }

        private var scanProgress = 0f
        private var animator: ValueAnimator? = null

        fun startScan() {
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1600
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener {
                    scanProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun stopScan() {
            animator?.cancel()
            animator = null
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            if (w <= 0 || h <= 0) return

            // Scan area margins
            val left = 32f
            val top = 80f
            val right = w - 32f
            val bottom = h - 80f

            // Corner brackets
            val bracketLen = 40f
            // Top Left
            canvas.drawLine(left, top, left + bracketLen, top, bracketPaint)
            canvas.drawLine(left, top, left, top + bracketLen, bracketPaint)
            // Top Right
            canvas.drawLine(right, top, right - bracketLen, top, bracketPaint)
            canvas.drawLine(right, top, right, top + bracketLen, bracketPaint)
            // Bottom Left
            canvas.drawLine(left, bottom, left + bracketLen, bottom, bracketPaint)
            canvas.drawLine(left, bottom, left, bottom - bracketLen, bracketPaint)
            // Bottom Right
            canvas.drawLine(right, bottom, right - bracketLen, bottom, bracketPaint)
            canvas.drawLine(right, bottom, right, bottom - bracketLen, bracketPaint)

            // Laser scan line position
            val scanY = top + (bottom - top) * scanProgress

            // Glow shader
            glowPaint.shader = LinearGradient(
                0f, scanY - 30f, 0f, scanY + 30f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#4D00E676"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(left, scanY - 30f, right, scanY + 30f, glowPaint)

            // Crisp laser line
            canvas.drawLine(left, scanY, right, scanY, laserPaint)
        }
    }
}

