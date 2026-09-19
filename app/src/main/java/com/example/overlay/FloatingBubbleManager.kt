package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.example.model.BubbleState
import com.example.model.MarketAnalysisResult

class FloatingBubbleManager(
    private val context: Context,
    private val onTriggerAnalysis: () -> Unit,
    private val onStopService: () -> Unit,
    private val onOpenSimulator: () -> Unit = {}
) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var bubbleView: FloatingBubbleView? = null
    private var overlayView: AnalyzerOverlayView? = null
    private var isBubbleAttached = false
    private var isOverlayAttached = false

    fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun showBubble() {
        if (!canDrawOverlays()) {
            Log.w("FloatingBubbleManager", "Overlay permission not granted")
            return
        }

        mainHandler.post {
            if (isBubbleAttached) return@post

            val bubbleSize = (64 * context.resources.displayMetrics.density).toInt()
            val layoutParams = WindowManager.LayoutParams(
                bubbleSize,
                bubbleSize,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (context.resources.displayMetrics.widthPixels * 0.82f).toInt()
                y = (context.resources.displayMetrics.heightPixels * 0.35f).toInt()
            }

            bubbleView = FloatingBubbleView(
                context = context,
                windowManager = windowManager,
                layoutParams = layoutParams,
                onTap = {
                    // Start screen analysis without covering screen yet
                    setBubbleState(BubbleState.ANALYZING)
                    onTriggerAnalysis()
                },
                onLongPress = {
                    showQuickMenu()
                }
            )

            try {
                windowManager.addView(bubbleView, layoutParams)
                isBubbleAttached = true
            } catch (e: Exception) {
                Log.e("FloatingBubbleManager", "Failed to add bubble: ${e.message}", e)
            }
        }
    }

    fun setBubbleState(state: BubbleState) {
        mainHandler.post {
            bubbleView?.state = state
        }
    }

    /**
     * Temporarily hides or restores the overlay so screen capture captures clean chart
     */
    fun setOverlayInvisible(invisible: Boolean) {
        mainHandler.post {
            overlayView?.setInvisibleForCapture(invisible)
        }
    }

    fun showScanningOverlay() {
        mainHandler.post {
            if (!canDrawOverlays()) return@post

            if (!isOverlayAttached) {
                val overlayParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.TYPE_PHONE
                    },
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                )

                overlayView = AnalyzerOverlayView(
                    context = context,
                    onCancel = {
                        hideOverlay()
                        setBubbleState(BubbleState.READY)
                    },
                    onAnalyzeAgain = {
                        setBubbleState(BubbleState.ANALYZING)
                        onTriggerAnalysis()
                    },
                    onClose = {
                        hideOverlay()
                        setBubbleState(BubbleState.READY)
                    },
                    onOpenSimulator = {
                        hideOverlay()
                        setBubbleState(BubbleState.READY)
                        onOpenSimulator()
                    }
                )

                try {
                    windowManager.addView(overlayView, overlayParams)
                    isOverlayAttached = true
                } catch (e: Exception) {
                    Log.e("FloatingBubbleManager", "Failed to add overlay: ${e.message}", e)
                }
            } else {
                overlayView?.setInvisibleForCapture(false)
                overlayView?.showScanningState()
            }
        }
    }

    fun showAnalysisResult(result: MarketAnalysisResult) {
        mainHandler.post {
            bubbleView?.state = BubbleState.RESULT
            bubbleView?.lastSignal = result.signal

            if (!isOverlayAttached) {
                showScanningOverlay()
            }
            overlayView?.setInvisibleForCapture(false)
            overlayView?.showResult(result)
        }
    }

    fun showAnalysisError(message: String) {
        mainHandler.post {
            bubbleView?.state = BubbleState.ERROR

            if (!isOverlayAttached) {
                showScanningOverlay()
            }
            overlayView?.setInvisibleForCapture(false)
            overlayView?.showError(message)
        }
    }

    fun hideOverlay() {
        mainHandler.post {
            if (isOverlayAttached && overlayView != null) {
                try {
                    windowManager.removeView(overlayView)
                } catch (e: Exception) {
                    Log.e("FloatingBubbleManager", "Failed to remove overlay: ${e.message}")
                }
                overlayView = null
                isOverlayAttached = false
            }
        }
    }

    fun removeBubble() {
        mainHandler.post {
            hideOverlay()
            if (isBubbleAttached && bubbleView != null) {
                try {
                    windowManager.removeView(bubbleView)
                } catch (e: Exception) {
                    Log.e("FloatingBubbleManager", "Failed to remove bubble: ${e.message}")
                }
                bubbleView = null
                isBubbleAttached = false
            }
        }
    }

    private fun showQuickMenu() {
        Toast.makeText(
            context,
            "AI Analyzer: Tap bubble to scan chart",
            Toast.LENGTH_SHORT
        ).show()
    }
}
