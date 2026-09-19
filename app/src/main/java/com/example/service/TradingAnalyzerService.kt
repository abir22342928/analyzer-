package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.analysis.TechnicalAnalysisEngine
import com.example.capture.ScreenCaptureManager
import com.example.database.AppDatabase
import com.example.database.HistoryRepository
import com.example.model.BubbleState
import com.example.model.MarketAnalysisResult
import com.example.overlay.FloatingBubbleManager
import com.example.vision.ChartDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TradingAnalyzerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var screenCaptureManager: ScreenCaptureManager? = null
    private var floatingBubbleManager: FloatingBubbleManager? = null
    private var historyRepository: HistoryRepository? = null
    private var autoAnalysisJob: Job? = null

    companion object {
        const val CHANNEL_ID = "trading_analyzer_service_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TRIGGER_ANALYSIS = "ACTION_TRIGGER_ANALYSIS"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_DATA_INTENT = "EXTRA_DATA_INTENT"

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _latestResult = MutableStateFlow<MarketAnalysisResult?>(null)
        val latestResult = _latestResult.asStateFlow()

        private val _bubbleState = MutableStateFlow(BubbleState.STOPPED)
        val bubbleState = _bubbleState.asStateFlow()

        var autoAnalysisIntervalSec: Int = 0 // 0 = manual, >0 = interval
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val db = AppDatabase.getDatabase(this)
        historyRepository = HistoryRepository(db.analysisHistoryDao())

        floatingBubbleManager = FloatingBubbleManager(
            context = this,
            onTriggerAnalysis = {
                performScreenAnalysis()
            },
            onStopService = {
                stopSelf()
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA_INTENT)
                }

                if (resultCode != 0 && data != null) {
                    startForegroundServiceWithNotification()
                    setupMediaProjection(resultCode, data)
                    floatingBubbleManager?.showBubble()
                    _isRunning.value = true
                    _bubbleState.value = BubbleState.READY
                    floatingBubbleManager?.setBubbleState(BubbleState.READY)

                    checkAndStartAutoAnalysis()
                } else {
                    Log.e("TradingAnalyzerService", "Missing media projection extras")
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                cleanupAndStop()
            }

            ACTION_TRIGGER_ANALYSIS -> {
                performScreenAnalysis()
            }
        }

        return START_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Trading Analyzer Active")
            .setContentText("Floating bubble running. Tap bubble on chart to analyze.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        if (mediaProjection == null) {
            Log.e("TradingAnalyzerService", "MediaProjection is null")
            stopSelf()
            return
        }

        val metrics = resources.displayMetrics
        screenCaptureManager = ScreenCaptureManager(
            mediaProjection = mediaProjection,
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            densityDpi = metrics.densityDpi
        )
    }

    fun performScreenAnalysis() {
        _bubbleState.value = BubbleState.ANALYZING
        floatingBubbleManager?.setBubbleState(BubbleState.ANALYZING)

        serviceScope.launch {
            // Briefly delay to let overlay / scrim settle if needed
            delay(350)

            val frameBitmap = withContext(Dispatchers.IO) {
                screenCaptureManager?.captureFrame()
            }

            if (frameBitmap == null) {
                // If direct screen capture is null (e.g. system buffer not ready yet), try once more
                delay(200)
                val retryBitmap = withContext(Dispatchers.IO) {
                    screenCaptureManager?.captureFrame()
                }

                if (retryBitmap == null) {
                    floatingBubbleManager?.showAnalysisError("Screen capture frame is unavailable. Please try again.")
                    _bubbleState.value = BubbleState.ERROR
                    return@launch
                }
                processBitmap(retryBitmap)
            } else {
                processBitmap(frameBitmap)
            }
        }
    }

    private suspend fun processBitmap(bitmap: Bitmap) {
        val detectionResult = withContext(Dispatchers.Default) {
            ChartDetector.detectChart(bitmap)
        }

        // Free bitmap memory
        bitmap.recycle()

        if (!detectionResult.detected) {
            floatingBubbleManager?.showAnalysisError(
                detectionResult.errorMessage
                    ?: "Trading chart could not be detected. Please make the chart larger and try again."
            )
            _bubbleState.value = BubbleState.ERROR
            return
        }

        val analysisResult = withContext(Dispatchers.Default) {
            TechnicalAnalysisEngine.analyzeChart(detectionResult)
        }

        _latestResult.value = analysisResult
        _bubbleState.value = BubbleState.RESULT
        floatingBubbleManager?.showAnalysisResult(analysisResult)

        // Haptic feedback
        triggerHapticFeedback()

        // Persist in Room Database
        withContext(Dispatchers.IO) {
            historyRepository?.saveResult(analysisResult)
        }
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(120)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    private fun checkAndStartAutoAnalysis() {
        autoAnalysisJob?.cancel()
        if (autoAnalysisIntervalSec > 0) {
            autoAnalysisJob = serviceScope.launch {
                while (isActive) {
                    delay(autoAnalysisIntervalSec * 1000L)
                    performScreenAnalysis()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trading Analyzer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the screen analyzer floating bubble active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun cleanupAndStop() {
        autoAnalysisJob?.cancel()
        autoAnalysisJob = null
        floatingBubbleManager?.removeBubble()
        floatingBubbleManager = null
        screenCaptureManager?.release()
        screenCaptureManager = null
        _isRunning.value = false
        _bubbleState.value = BubbleState.STOPPED
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cleanupAndStop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
