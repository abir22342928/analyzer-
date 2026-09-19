package com.example.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.delay
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

class ScreenCaptureManager(
    private val mediaProjection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int
) {
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val isRunning = AtomicBoolean(true)
    private val latestBitmapRef = AtomicReference<Bitmap?>(null)
    private var lastCaptureTimestamp = 0L

    init {
        setupBackgroundThread()
        setupMediaProjectionCallback()
        setupVirtualDisplay()
    }

    private fun setupBackgroundThread() {
        handlerThread = HandlerThread("ScreenCaptureBackgroundThread").apply { start() }
        backgroundHandler = Handler(handlerThread!!.looper)
    }

    private fun setupMediaProjectionCallback() {
        try {
            mediaProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    Log.d("ScreenCaptureManager", "MediaProjection stopped by system")
                    isRunning.set(false)
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.w("ScreenCaptureManager", "Could not register MediaProjection callback: ${e.message}")
        }
    }

    private fun setupVirtualDisplay() {
        try {
            // Keep actual aspect ratio while capping maximum dimension to prevent OOM
            val maxDimension = 1280
            val maxScreenDim = max(width, height)
            val scale = if (maxScreenDim > maxDimension) {
                maxDimension.toFloat() / maxScreenDim
            } else {
                1.0f
            }

            // Ensure dimensions are even numbers for buffer compatibility
            val captureWidth = ((width * scale).toInt() / 2) * 2
            val captureHeight = ((height * scale).toInt() / 2) * 2
            val captureDpi = (densityDpi * scale).toInt().coerceAtLeast(120)

            imageReader = ImageReader.newInstance(
                captureWidth,
                captureHeight,
                PixelFormat.RGBA_8888,
                3
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                if (!isRunning.get()) return@setOnImageAvailableListener
                try {
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val bitmap = processImageToBitmap(image)
                    image.close()

                    if (bitmap != null) {
                        val oldBitmap = latestBitmapRef.getAndSet(bitmap)
                        oldBitmap?.recycle()
                        lastCaptureTimestamp = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    Log.e("ScreenCaptureManager", "Error in onImageAvailable: ${e.message}")
                }
            }, backgroundHandler)

            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "TradingAnalyzerDisplay",
                captureWidth,
                captureHeight,
                captureDpi,
                flags,
                imageReader?.surface,
                null,
                backgroundHandler
            )

            Log.i("ScreenCaptureManager", "VirtualDisplay initialized: ${captureWidth}x${captureHeight} @ ${captureDpi}dpi")
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "Error setting up VirtualDisplay: ${e.message}", e)
        }
    }

    private fun processImageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            if (planes.isEmpty()) return null

            val buffer: ByteBuffer = planes[0].buffer
            buffer.rewind()

            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding == 0) {
                bitmap
            } else {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                bitmap.recycle()
                cropped
            }
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "Failed to process Image to Bitmap: ${e.message}")
            null
        }
    }

    /**
     * Captures a single frame as a Bitmap safely.
     * Waits asynchronously up to [timeoutMs] if no frame has arrived yet.
     */
    suspend fun captureFrame(timeoutMs: Long = 2000L): Bitmap? {
        val startTime = System.currentTimeMillis()

        // 1. If we already have a recent frame (under 1.5 seconds old), return a safe copy
        val cached = latestBitmapRef.get()
        if (cached != null && !cached.isRecycled && (startTime - lastCaptureTimestamp) < 1500L) {
            return try {
                cached.copy(Bitmap.Config.ARGB_8888, false)
            } catch (e: Exception) {
                null
            }
        }

        // 2. Otherwise, poll until a new frame arrives via the listener
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val current = latestBitmapRef.get()
            if (current != null && !current.isRecycled && lastCaptureTimestamp >= startTime) {
                return try {
                    current.copy(Bitmap.Config.ARGB_8888, false)
                } catch (e: Exception) {
                    null
                }
            }

            // Direct fallback attempt from reader if listener hasn't fired yet
            val directImage = try {
                imageReader?.acquireLatestImage() ?: imageReader?.acquireNextImage()
            } catch (e: Exception) {
                null
            }

            if (directImage != null) {
                val directBitmap = processImageToBitmap(directImage)
                directImage.close()
                if (directBitmap != null) {
                    val oldBitmap = latestBitmapRef.getAndSet(directBitmap)
                    oldBitmap?.recycle()
                    lastCaptureTimestamp = System.currentTimeMillis()
                    return directBitmap.copy(Bitmap.Config.ARGB_8888, false)
                }
            }

            delay(40)
        }

        // 3. If timeout reached, return whatever latest valid bitmap we have, even if older
        val fallback = latestBitmapRef.get()
        if (fallback != null && !fallback.isRecycled) {
            return try {
                fallback.copy(Bitmap.Config.ARGB_8888, false)
            } catch (e: Exception) {
                null
            }
        }

        Log.w("ScreenCaptureManager", "captureFrame timed out after ${timeoutMs}ms without a frame")
        return null
    }

    fun release() {
        isRunning.set(false)
        try {
            val lastBmp = latestBitmapRef.getAndSet(null)
            lastBmp?.recycle()

            virtualDisplay?.release()
            virtualDisplay = null

            imageReader?.close()
            imageReader = null

            handlerThread?.quitSafely()
            handlerThread = null
            backgroundHandler = null

            mediaProjection.stop()
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "Error releasing ScreenCaptureManager resources: ${e.message}")
        }
    }
}
